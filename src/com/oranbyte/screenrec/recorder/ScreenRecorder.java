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
	private static final long MIX_STALL_TIMEOUT_MS = 20;
	private static final int MIN_MIX_FRAMES = 256;
	private static final int MAX_MIX_FRAMES = 2048;

	private final Rectangle captureArea;
	private final String outputFileName;
	private MainFrame mainFrame;
	private volatile boolean isRecording;
	private volatile boolean isPaused;

	private final Object pauseLock = new Object();
	private final Object writerLock = new Object();

	private volatile boolean isMicrophoneEnabled = false;
	private volatile boolean isSpeakerEnabled = true;

	private static volatile int TARGET_FPS = AppConstant.FPS;

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
	
	private volatile boolean isCancelled = false;

	private double systemResamplePos = 0.0;

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
		if (isRecording)
			return;
		isRecording = true;
		isPaused = false;
		framesEncoded.set(0);
		totalPausedTime.set(0);
		systemResamplePos = 0.0;

		startTime = System.nanoTime();

		try {
			writer = ToolFactory.makeWriter(outputFileName);
			writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, captureArea.width, captureArea.height);

			// Always attach audio stream to enable dynamic audio toggling mid-session
			writer.addAudioStream(AUDIO_STREAM_INDEX, AUDIO_STREAM_INDEX, ICodec.ID.CODEC_ID_AAC, AUDIO_CHANNELS,
					AUDIO_SAMPLE_RATE);
			audioStreamAdded = true;
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
		synchronized (pauseLock) {
			if (isRecording && !isPaused) {
				isPaused = true;
				pauseStartedAt = System.nanoTime();
			}
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
				if (!isRecording)
					break;

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
			if (snapshot == null || snapshot.image == null)
				return;

			int screenX = snapshot.screenX - captureArea.x;
			int screenY = snapshot.screenY - captureArea.y;
			int drawX = screenX - snapshot.hotspotX;
			int drawY = screenY - snapshot.hotspotY;

			g.drawImage(snapshot.image, drawX, drawY, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void recordAudio() {
		int bytesPerSample = 2;
		int ringCapacity = AUDIO_SAMPLE_RATE * bytesPerSample * RING_BUFFER_SECONDS;
 
		micRing = new CircularByteBuffer(ringCapacity);
		micCaptureThread = new Thread(this::captureMic, "Screen Recorder - Mic Capture");
		micCaptureThread.setPriority(Thread.NORM_PRIORITY + 2);
		micCaptureThread.start();

		if (systemAudioSource != null) {
			systemRing = new CircularByteBuffer(ringCapacity);
			systemCaptureThread = new Thread(this::captureSystemAudio, "Screen Recorder - System Audio Capture");
			systemCaptureThread.setPriority(Thread.NORM_PRIORITY + 2);
			systemCaptureThread.start();
		}

		int maxChunkBytes = MAX_MIX_FRAMES * bytesPerSample;
		byte[] micChunk = new byte[maxChunkBytes];
		byte[] sysChunk = systemRing != null ? new byte[maxChunkBytes] : null;

		long audioClockNanos = currentTimestampNanos();

		try {
			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording)
					break;

				long nowNanos = currentTimestampNanos();
				long elapsedNanos = nowNanos - audioClockNanos;

				int framesNeeded = (int) (elapsedNanos * AUDIO_SAMPLE_RATE / 1_000_000_000L);

				if (framesNeeded < MIN_MIX_FRAMES) {
					Thread.sleep(2);
					continue;
				}
				framesNeeded = Math.min(framesNeeded, MAX_MIX_FRAMES);
				int chunkBytes = framesNeeded * bytesPerSample;

				int maxBacklogBytes = (AUDIO_SAMPLE_RATE / 2) * bytesPerSample;
				if (micRing.availableBytes() > maxBacklogBytes) {
					micRing.skip(micRing.availableBytes() - maxBacklogBytes);
				}
				if (systemRing != null && systemRing.availableBytes() > maxBacklogBytes) {
					systemRing.skip(systemRing.availableBytes() - maxBacklogBytes);
				}

				short[] micSamples = null;
				short[] systemSamples = null;

				int gotMic = micRing.read(micChunk, 0, chunkBytes, MIX_STALL_TIMEOUT_MS);
				if (gotMic < chunkBytes) {
					java.util.Arrays.fill(micChunk, Math.max(gotMic, 0), chunkBytes, (byte) 0);
				}
 
				if (isMicrophoneEnabled) {
					micSamples = bytesToShorts(micChunk, chunkBytes);
				} else {
					micSamples = new short[framesNeeded];  
				}

				if (systemRing != null) {
					int gotSys = systemRing.read(sysChunk, 0, chunkBytes, MIX_STALL_TIMEOUT_MS);
					if (gotSys < chunkBytes) {
						java.util.Arrays.fill(sysChunk, Math.max(gotSys, 0), chunkBytes, (byte) 0);
					}
 
					if (isSpeakerEnabled) {
						systemSamples = bytesToShorts(sysChunk, chunkBytes);
					} else {
						systemSamples = new short[framesNeeded];  
					}
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
					synchronized (writerLock) {
						writer.encodeAudio(AUDIO_STREAM_INDEX, outSamples, audioClockNanos, TimeUnit.NANOSECONDS);
					}
				}

				audioClockNanos += (framesNeeded * 1_000_000_000L) / AUDIO_SAMPLE_RATE;
			}
		} catch (Exception e) {
			System.err.println("Error during audio mixing: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (micCaptureThread != null)
					micCaptureThread.join(1000);
				if (systemCaptureThread != null)
					systemCaptureThread.join(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (micRing != null)
				micRing.markFinished();
			if (systemRing != null)
				systemRing.markFinished();
		}
	}

	private void captureMic() {
		try {
			micLine = openMic();
			byte[] raw = new byte[AUDIO_BUFFER_BYTES];

			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording)
					break;

				int read = micLine.read(raw, 0, raw.length);
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

			byte[] raw = new byte[8192];

			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording)
					break;

				int read = systemAudioSource.read(raw, 0, raw.length);
				if (read > 0) {
					short[] normalized = normalizeToTargetFormat(raw, read, systemAudioNativeFormat);
					if (normalized.length > 0) {
						byte[] outBytes = shortsToBytes(normalized);
						systemRing.write(outBytes, 0, outBytes.length);
					}
				} else {
					Thread.sleep(2);
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
				return (AudioFormat) result;
			}
		} catch (Exception ignored) {
		}
		return new AudioFormat(48000.0f, 16, 2, true, false);
	}

	private short[] normalizeToTargetFormat(byte[] raw, int length, AudioFormat nativeFormat) {
		if (nativeFormat == null || length <= 0) {
			return new short[0];
		}

		int channels = nativeFormat.getChannels();
		float srcRate = nativeFormat.getSampleRate();
		int sampleSizeInBits = nativeFormat.getSampleSizeInBits();

		short[] pcmShorts;

		if (nativeFormat.getEncoding() == AudioFormat.Encoding.PCM_FLOAT || sampleSizeInBits == 32) {
			int sampleCount = length / 4;
			pcmShorts = new short[sampleCount];
			for (int i = 0; i < sampleCount; i++) {
				int intBits = (raw[i * 4] & 0xFF) | ((raw[i * 4 + 1] & 0xFF) << 8) | ((raw[i * 4 + 2] & 0xFF) << 16)
						| ((raw[i * 4 + 3] & 0xFF) << 24);
				float f = Float.intBitsToFloat(intBits);
				f = Math.max(-1.0f, Math.min(1.0f, f));
				pcmShorts[i] = (short) (f * 32767.0f);
			}
		} else {
			pcmShorts = bytesToShorts(raw, length);
		}

		short[] mono = (channels <= 1) ? pcmShorts : downmixToMono(pcmShorts, channels);

		if (Math.abs(srcRate - AUDIO_SAMPLE_RATE) < 1.0f) {
			return mono;
		}

		return resampleLinearPersistent(mono, srcRate, AUDIO_SAMPLE_RATE);
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

	private short[] resampleLinearPersistent(short[] input, float srcRate, float dstRate) {
		if (input.length == 0)
			return input;

		double ratio = srcRate / dstRate;
		int expectedLength = (int) Math.floor(input.length / ratio);
		short[] output = new short[expectedLength];

		int outIdx = 0;
		while (outIdx < expectedLength) {
			int idx0 = (int) Math.floor(systemResamplePos);
			int idx1 = idx0 + 1;

			if (idx1 >= input.length)
				break;

			double frac = systemResamplePos - idx0;
			output[outIdx++] = (short) Math.round(input[idx0] * (1.0 - frac) + input[idx1] * frac);

			systemResamplePos += ratio;
		}

		systemResamplePos -= Math.floor(systemResamplePos);
		if (outIdx == output.length) {
			return output;
		}
		short[] trimmed = new short[outIdx];
		System.arraycopy(output, 0, trimmed, 0, outIdx);
		return trimmed;
	}

	private TargetDataLine openMic() throws Exception {
		AudioFormat format = new AudioFormat(AUDIO_SAMPLE_RATE, 16, AUDIO_CHANNELS, true, false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		if (!AudioSystem.isLineSupported(info)) {
			throw new IllegalStateException("No microphone line available for format " + format);
		}

		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(format, AUDIO_BUFFER_BYTES * 4);
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
			if (sum > Short.MAX_VALUE)
				sum = Short.MAX_VALUE;
			else if (sum < Short.MIN_VALUE)
				sum = Short.MIN_VALUE;
			out[i] = (short) sum;
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
	        if (videoThread != null)
	            videoThread.join();
	        if (audioThread != null)
	            audioThread.join();
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

	    if (isCancelled) {
	        return;
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
	 
	public String getOutputFileName() {
	    return outputFileName;
	}

	public File getOutputFile() {
	    return outputFileName != null ? new File(outputFileName) : null;
	}
	
	public void cancel() {
	    this.isCancelled = true;
	    stop();
	}
}