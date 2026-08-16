package com.oranbyte.screenrec.recorder;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import com.oranbyte.screenrec.audio.SystemAudioSource;
import com.oranbyte.screenrec.audio.WasapiAudioSource;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.gui.MainFrame;
import com.oranbyte.screenrec.util.CursorUtils;
import com.oranbyte.screenrec.util.NotificationUtil;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class ScreenRecorder {

	private static final int AUDIO_SAMPLE_RATE = 44100;
	private static final int AUDIO_CHANNELS = 1;
	private static final int AUDIO_STREAM_INDEX = 1;
	private static final int AUDIO_BUFFER_BYTES = 4096;
	private static final int RING_BUFFER_SECONDS = 5;
	private static final long MIX_STALL_TIMEOUT_MS = 40; 
	private static final int MIN_MIX_FRAMES = 256;
	private static final int MAX_MIX_FRAMES = AUDIO_SAMPLE_RATE;

	private final Rectangle captureArea;
	private final String outputFileName;
	private MainFrame mainFrame;
	private volatile boolean isRecording;
	private volatile boolean isPaused;

	private final Object pauseLock = new Object();
	private final Object writerLock = new Object();

	private volatile boolean isMicrophoneEnabled = false;
	private volatile boolean isSpeakerEnabled = true;

	private static volatile int TARGET_FPS = 50;
 
	private volatile long startTime;
	private final AtomicLong totalPausedTime = new AtomicLong(0);
	private volatile long pauseStartedAt = 0;

	private IMediaWriter writer;
	private boolean audioStreamAdded;

	private TargetDataLine micLine;
	private SystemAudioSource systemAudioSource;

	private AudioFormat systemAudioNativeFormat;

	private Thread videoThread;
	private Thread audioThread;
	private Thread micCaptureThread;
	private Thread systemCaptureThread;

	private CircularByteBuffer micRing;
	private CircularByteBuffer systemRing;

	private final AtomicInteger framesEncoded = new AtomicInteger(0);
	private final CountDownLatch captureStarted = new CountDownLatch(1);

	public ScreenRecorder(Rectangle captureArea) {

		int width = captureArea.width % 2 == 0 ? captureArea.width : captureArea.width - 1;
		int height = captureArea.height % 2 == 0 ? captureArea.height : captureArea.height - 1;

		this.captureArea = new Rectangle(captureArea.x, captureArea.y, width, height);

		File saveDir = new File(AppConstant.SAVE_LOCATION);
		if (!saveDir.exists()) {
			saveDir.mkdirs();
		}
		this.systemAudioSource = new WasapiAudioSource(WasapiAudioSource.Mode.LOOPBACK);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss");
		String timestamp = LocalDateTime.now().format(formatter);

		this.outputFileName = AppConstant.SAVE_LOCATION + File.separator + "Recording " + timestamp + ".mp4";
	}

	public ScreenRecorder(Rectangle captureArea, boolean isMicrophoneEnabled, boolean isSpeakerEnabled) {
		this(captureArea);

		this.isMicrophoneEnabled = isMicrophoneEnabled;
		this.isSpeakerEnabled = isSpeakerEnabled;
	}

	public ScreenRecorder(MainFrame mainFrame, Rectangle captureArea, boolean isMicrophoneEnabled,
			boolean isSpeakerEnabled) {
		this(captureArea, isMicrophoneEnabled, isSpeakerEnabled);
		this.mainFrame = mainFrame;
	}

	public void setMicrophoneEnabled(boolean enabled) {
		this.isMicrophoneEnabled = enabled;
	}

	public void setSpeakerEnabled(boolean enabled) {
		this.isSpeakerEnabled = enabled;
	}

	public void setSystemAudioSource(SystemAudioSource source) {
		this.systemAudioSource = source;
	}

	public void start() {
		System.out.println("mic=" + isMicrophoneEnabled + " speaker=" + isSpeakerEnabled + " audioStreamAdded="
				+ audioStreamAdded);
		if (isRecording)
			return;
		isRecording = true;
		isPaused = false;
		framesEncoded.set(0);
		totalPausedTime.set(0);
//		lastAudioPtsNanos.set(-1);
 
		startTime = System.nanoTime();

		try {
			writer = ToolFactory.makeWriter(outputFileName);
			writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, captureArea.width, captureArea.height);

			boolean wantsAudio = isMicrophoneEnabled || (isSpeakerEnabled && systemAudioSource != null);
			if (wantsAudio) {
				writer.addAudioStream(AUDIO_STREAM_INDEX, AUDIO_STREAM_INDEX, ICodec.ID.CODEC_ID_AAC, AUDIO_CHANNELS,
						AUDIO_SAMPLE_RATE);
				audioStreamAdded = true;
			}
		} catch (Exception e) {
			System.err.println("Failed to initialize writer: " + e.getMessage());
			e.printStackTrace();
			isRecording = false;
			return;
		}

		videoThread = new Thread(this::recordScreen, "Screen Recorder - Video");
		videoThread.start();

		if (audioStreamAdded) {
			audioThread = new Thread(this::recordAudio, "Screen Recorder - Audio Mixer");
			audioThread.start();
		}

		new Thread(this::finalizeWhenDone, "Screen Recorder - Finalizer").start();

		try {
			captureStarted.await(3, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}

	public void stop() {
		isRecording = false;
		resume();
	}

	public void pause() {
		if (isRecording && !isPaused) {
			isPaused = true;
			pauseStartedAt = System.nanoTime();
		}
	}

	public void resume() {
		synchronized (pauseLock) {
			if (isPaused) {
				totalPausedTime.addAndGet(System.nanoTime() - pauseStartedAt);
			}
			isPaused = false;
			pauseLock.notifyAll();
		}
	}

	public boolean isPaused() {
		return isPaused;
	}

	public boolean isRecording() {
		return isRecording;
	}

	public static void setTargetFps(int fps) {
		if (fps < 1)
			fps = 1;
		if (fps > 60)
			fps = 60;
		TARGET_FPS = fps;
	}

	public static int getTargetFps() {
		return TARGET_FPS;
	}

	private void recordScreen() {
		try {
			Robot robot = new Robot();
 
			captureStarted.countDown();

			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording) {
					break;
				}

				long frameStart = System.currentTimeMillis();
				long frameIntervalMs = 1000L / TARGET_FPS;

				BufferedImage image = robot.createScreenCapture(captureArea);

				BufferedImage bgrImage = new BufferedImage(image.getWidth(), image.getHeight(),
						BufferedImage.TYPE_3BYTE_BGR);

				Graphics2D g = bgrImage.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.drawImage(image, 0, 0, null);

				drawCursor(g);

				g.dispose();

				long frameTimeStamp = currentTimestampNanos();
				synchronized (writerLock) {
					writer.encodeVideo(0, bgrImage, frameTimeStamp, TimeUnit.NANOSECONDS);
				}
				framesEncoded.incrementAndGet();

				long timeTaken = System.currentTimeMillis() - frameStart;
				long sleepTime = frameIntervalMs - timeTaken;
				if (sleepTime > 0) {
					Thread.sleep(sleepTime);
				}
			}
		} catch (Exception e) {
			captureStarted.countDown();
			System.err.println("Error during recording: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void drawCursor(Graphics2D g) {
		try {
			CursorUtils.CursorSnapshot snapshot = CursorUtils.capture();
			if (snapshot == null || snapshot.image == null) {
				return;
			}

			int screenX = snapshot.screenX - captureArea.x;
			int screenY = snapshot.screenY - captureArea.y;

			int drawX = screenX - snapshot.hotspotX;
			int drawY = screenY - snapshot.hotspotY;

			g.drawImage(snapshot.image, drawX, drawY, null);
		} catch (Exception e) {
			// IGNOREING...
			System.out.println("error : " + e.getMessage());
		}
	}

	private void recordAudio() {
		int bytesPerSample = 2;
		int ringCapacity = AUDIO_SAMPLE_RATE * bytesPerSample * RING_BUFFER_SECONDS;

		if (isMicrophoneEnabled) {
			micRing = new CircularByteBuffer(ringCapacity);
			micCaptureThread = new Thread(this::captureMic, "Screen Recorder - Mic Capture"); 
			micCaptureThread.setPriority(Thread.NORM_PRIORITY + 2);
			micCaptureThread.start();
		}

		if (isSpeakerEnabled && systemAudioSource != null) {
			systemRing = new CircularByteBuffer(ringCapacity);
			systemCaptureThread = new Thread(this::captureSystemAudio, "Screen Recorder - System Audio Capture");
			systemCaptureThread.setPriority(Thread.NORM_PRIORITY + 2);
			systemCaptureThread.start();
		}

		System.out.println("mic ring bytes: " + (micRing != null) + ", sys ring bytes: " + (systemRing != null));

		int maxChunkBytes = MAX_MIX_FRAMES * bytesPerSample;
		byte[] micChunk = micRing != null ? new byte[maxChunkBytes] : null;
		byte[] sysChunk = systemRing != null ? new byte[maxChunkBytes] : null;

		 
		long audioClockNanos = currentTimestampNanos();

		try {
			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording) {
					break;
				}

				long nowNanos = currentTimestampNanos();
				long elapsedNanos = nowNanos - audioClockNanos;
				int framesNeeded = (int) (elapsedNanos * AUDIO_SAMPLE_RATE / 1_000_000_000L);

				if (framesNeeded < MIN_MIX_FRAMES) { 
					Thread.sleep(3);
					continue;
				}
				framesNeeded = Math.min(framesNeeded, MAX_MIX_FRAMES);

				int chunkBytes = framesNeeded * bytesPerSample;

				short[] micSamples = null;
				short[] systemSamples = null;

				if (micRing != null) {
					int gotMic = micRing.read(micChunk, 0, chunkBytes, MIX_STALL_TIMEOUT_MS);
					if (gotMic < chunkBytes) {
						java.util.Arrays.fill(micChunk, Math.max(gotMic, 0), chunkBytes, (byte) 0);
					}
					micSamples = bytesToShorts(micChunk, chunkBytes);
				}

				if (systemRing != null) {
					int gotSys = systemRing.read(sysChunk, 0, chunkBytes, MIX_STALL_TIMEOUT_MS);
					if (gotSys < chunkBytes) {
						java.util.Arrays.fill(sysChunk, Math.max(gotSys, 0), chunkBytes, (byte) 0);
					}
					systemSamples = bytesToShorts(sysChunk, chunkBytes);
				}

				short[] outSamples;
				if (micSamples != null && systemSamples != null) {
					outSamples = mix(micSamples, systemSamples);
				} else if (micSamples != null) {
					outSamples = micSamples;
				} else {
					outSamples = systemSamples;
				}

				if (outSamples != null && outSamples.length > 0) {
					long pts = audioClockNanos;

					synchronized (writerLock) {
						writer.encodeAudio(AUDIO_STREAM_INDEX, outSamples, pts, TimeUnit.NANOSECONDS);
					}
				}
 
				audioClockNanos += (framesNeeded * 1_000_000_000L) / AUDIO_SAMPLE_RATE;
			}
		} catch (Exception e) {
			System.err.println("Error during audio mixing: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (micCaptureThread != null) {
					micCaptureThread.join(2000);
				}
				if (systemCaptureThread != null) {
					systemCaptureThread.join(2000);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (micRing != null) {
				micRing.markFinished();
			}
			if (systemRing != null) {
				systemRing.markFinished();
			}
		}
	}

	private void captureMic() {
		try {
			micLine = openMic();
			byte[] raw = new byte[AUDIO_BUFFER_BYTES];

			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording) {
					break;
				}

				int availableBytes = micLine.available();
				if (availableBytes <= 0) {
					Thread.sleep(5);
					continue;
				}

				int read = micLine.read(raw, 0, Math.min(availableBytes, raw.length));
				if (read > 0) {
					micRing.write(raw, 0, read);
				}
			}
		} catch (Exception e) {
			System.err.println("Error capturing microphone audio: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (micLine != null) {
				micLine.stop();
				micLine.close();
			}
		}
	}

	private void captureSystemAudio() {
		HRESULT hr = null;
		boolean comInitialized = false;
		try {
			hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
			comInitialized = hr.intValue() >= 0;

			systemAudioSource.start(AUDIO_SAMPLE_RATE, AUDIO_CHANNELS);
			systemAudioNativeFormat = resolveSystemAudioFormat();

			byte[] raw = new byte[AUDIO_BUFFER_BYTES];

			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording) {
					break;
				}

				int read = systemAudioSource.read(raw, 0, raw.length);
				if (read > 0) {
					short[] normalized = normalizeToTargetFormat(raw, read, systemAudioNativeFormat);
					if (normalized.length > 0) {
						byte[] outBytes = shortsToBytes(normalized);
						systemRing.write(outBytes, 0, outBytes.length);
					}
				} else {
					Thread.sleep(5);
				}
			}
		} catch (Exception e) {
			System.err.println("Error capturing system audio: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (systemAudioSource != null) {
				systemAudioSource.stop();
			}
			if (comInitialized) {
				Ole32.INSTANCE.CoUninitialize();
			}
		}
	}

	private AudioFormat resolveSystemAudioFormat() {
		try {
			java.lang.reflect.Method m = systemAudioSource.getClass().getMethod("getCaptureFormat");
			Object result = m.invoke(systemAudioSource);
			if (result instanceof AudioFormat) {
				AudioFormat fmt = (AudioFormat) result;
				System.out.println("System audio native format: " + fmt);
				return fmt;
			}
		} catch (NoSuchMethodException ignored) {
			System.out.println("WARNING: SystemAudioSource does not expose getCaptureFormat(). "
					+ "Assuming captured audio is already mono/" + AUDIO_SAMPLE_RATE + "Hz/16-bit. "
					+ "If recordings run long, this assumption is likely wrong - "
					+ "add getCaptureFormat() to WasapiAudioSource.");
		} catch (Exception e) {
			System.err.println("Failed to resolve system audio format: " + e.getMessage());
		}
		return new AudioFormat(AUDIO_SAMPLE_RATE, 16, AUDIO_CHANNELS, true, false);
	}

	private short[] normalizeToTargetFormat(byte[] raw, int length, AudioFormat nativeFormat) {
		if (nativeFormat == null || length <= 0) {
			return new short[0];
		}

		int srcChannels = nativeFormat.getChannels();
		float srcRate = nativeFormat.getSampleRate();
		AudioFormat.Encoding encoding = nativeFormat.getEncoding();
		int sampleSizeInBits = nativeFormat.getSampleSizeInBits();

		short[] rawShorts;

		if (encoding == AudioFormat.Encoding.PCM_FLOAT || sampleSizeInBits == 32) {
			int floatCount = length / 4;
			rawShorts = new short[floatCount];
			for (int i = 0; i < floatCount; i++) {
				int intBits = (raw[i * 4] & 0xFF) | ((raw[i * 4 + 1] & 0xFF) << 8) | ((raw[i * 4 + 2] & 0xFF) << 16)
						| ((raw[i * 4 + 3] & 0xFF) << 24);
				float floatVal = Float.intBitsToFloat(intBits);
				floatVal = Math.max(-1.0f, Math.min(1.0f, floatVal));
				rawShorts[i] = (short) (floatVal * 32767.0f);
			}
		} else {
			rawShorts = bytesToShorts(raw, length);
		}

		short[] mono = (srcChannels <= 1) ? rawShorts : downmixToMono(rawShorts, srcChannels);
		if (Math.abs(srcRate - AUDIO_SAMPLE_RATE) < 1.0f) {
			return mono;
		}

		return resampleLinear(mono, srcRate, AUDIO_SAMPLE_RATE);
	}

	private short[] downmixToMono(short[] interleaved, int channels) {
		int frames = interleaved.length / channels;
		short[] mono = new short[frames];
		for (int i = 0; i < frames; i++) {
			int sum = 0;
			for (int c = 0; c < channels; c++) {
				sum += interleaved[i * channels + c];
			}
			mono[i] = (short) (sum / channels);
		}
		return mono;
	}

	private short[] resampleLinear(short[] input, float srcRate, float dstRate) {
		if (input.length == 0) {
			return input;
		}
		double ratio = dstRate / srcRate;
		int outLength = Math.max(1, (int) Math.round(input.length * ratio));
		short[] output = new short[outLength];

		for (int i = 0; i < outLength; i++) {
			double srcPos = i / ratio;
			int idx0 = (int) Math.floor(srcPos);
			int idx1 = Math.min(idx0 + 1, input.length - 1);
			idx0 = Math.min(idx0, input.length - 1);
			double frac = srcPos - idx0;
			output[i] = (short) Math.round(input[idx0] * (1.0 - frac) + input[idx1] * frac);
		}
		return output;
	}

	private TargetDataLine openMic() throws Exception {
		AudioFormat format = new AudioFormat(AUDIO_SAMPLE_RATE, 16, AUDIO_CHANNELS, true, false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		if (!AudioSystem.isLineSupported(info)) {
			throw new IllegalStateException("No microphone line available for format " + format);
		}

		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();
		return line;
	}

	private short[] bytesToShorts(byte[] bytes, int length) {
		short[] samples = new short[length / 2];
		for (int i = 0; i < samples.length; i++) {
			int lo = bytes[2 * i] & 0xFF;
			int hi = bytes[2 * i + 1];
			samples[i] = (short) ((hi << 8) | lo);
		}
		return samples;
	}

	private byte[] shortsToBytes(short[] samples) {
		byte[] bytes = new byte[samples.length * 2];
		for (int i = 0; i < samples.length; i++) {
			bytes[2 * i] = (byte) (samples[i] & 0xFF);
			bytes[2 * i + 1] = (byte) ((samples[i] >> 8) & 0xFF);
		}
		return bytes;
	}

	private short[] mix(short[] a, short[] b) {
		int len = Math.min(a.length, b.length);
		short[] out = new short[len];
		for (int i = 0; i < len; i++) {
			int sum = a[i] + b[i];
			out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sum));
		}
		return out;
	}

	private void awaitResumeIfPaused() throws InterruptedException {
		if (isPaused) {
			synchronized (pauseLock) {
				while (isPaused && isRecording) {
					pauseLock.wait();
				}
			}
		}
	}

	private long currentTimestampNanos() {
		return (System.nanoTime() - startTime) - totalPausedTime.get();
	}

	private void finalizeWhenDone() {
		try {
			if (videoThread != null) {
				videoThread.join();
			}

			if (audioThread != null) {
				audioThread.join();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		try {
			if (writer != null) {
				if (framesEncoded.get() > 0) {
					writer.close();
				} else {
					System.err.println("No frames were recorded; skipping writer.close()");
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to finalize video trailer: " + e.getMessage());
			e.printStackTrace();
		}

		NotificationUtil.notify("Video saved", outputFileName, new File(outputFileName),
				() -> System.out.println("clicked"));

		if (mainFrame != null) {
			javax.swing.SwingUtilities.invokeLater(() -> {
				try {
					javafx.application.Platform.runLater(() -> {
						try {
							mainFrame.setVideoPanel(outputFileName);
						} catch (Exception e) {
							System.err.println("Failed to load video preview panel: " + e.getMessage());
							e.printStackTrace();
						}
					});
				} catch (IllegalStateException e) {
					System.err.println("JavaFX toolkit is not initialized: " + e.getMessage());
				}
			});
		}
	}

}