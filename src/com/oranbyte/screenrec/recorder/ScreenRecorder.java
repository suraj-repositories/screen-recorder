package com.oranbyte.screenrec.recorder;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.util.CursorUtils;
import com.oranbyte.screenrec.util.NotificationUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class ScreenRecorder {

	private static final int AUDIO_SAMPLE_RATE = 44100;
	private static final int AUDIO_CHANNELS = 1;
	private static final int AUDIO_STREAM_INDEX = 1;
	private static final int AUDIO_BUFFER_BYTES = 4096;

	private final Rectangle captureArea;
	private final String outputFileName;

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

	private Thread videoThread;
	private Thread audioThread;

	private final AtomicInteger framesEncoded = new AtomicInteger(0);

	public ScreenRecorder(Rectangle captureArea) {

		int width = captureArea.width % 2 == 0 ? captureArea.width : captureArea.width - 1;
		int height = captureArea.height % 2 == 0 ? captureArea.height : captureArea.height - 1;

		this.captureArea = new Rectangle(captureArea.x, captureArea.y, width, height);

		File saveDir = new File(AppConstant.SAVE_LOCATION);
		if (!saveDir.exists()) {
			saveDir.mkdirs();
		}

		this.systemAudioSource = new JavaSoundAudioSource();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss");
		String timestamp = LocalDateTime.now().format(formatter);

		this.outputFileName = AppConstant.SAVE_LOCATION + File.separator + "Screen Recording " + timestamp + ".mp4";
	}

	public ScreenRecorder(Rectangle captureArea, boolean isMicrophoneEnabled, boolean isSpeakerEnabled) {
		this(captureArea);

		this.isMicrophoneEnabled = isMicrophoneEnabled;
		this.isSpeakerEnabled = isSpeakerEnabled;
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
		if (isRecording) {
			return;
		}
		isRecording = true;
		isPaused = false;
		framesEncoded.set(0);
		totalPausedTime.set(0);

		try {
			writer = ToolFactory.makeWriter(outputFileName);
			writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, captureArea.width, captureArea.height);

			boolean wantsAudio = isMicrophoneEnabled || (isSpeakerEnabled && systemAudioSource != null);

			if (wantsAudio) {
				writer.addAudioStream(AUDIO_STREAM_INDEX, AUDIO_STREAM_INDEX, ICodec.ID.CODEC_ID_AAC, AUDIO_CHANNELS,
						AUDIO_SAMPLE_RATE);
				audioStreamAdded = true;

				System.out.println("Recording audio...");
			}
		} catch (Exception e) {
			System.err.println("Failed to initialize writer: " + e.getMessage());
			e.printStackTrace();
			isRecording = false;
			return;
		}

		startTime = System.nanoTime();

		videoThread = new Thread(this::recordScreen, "Screen Recorder - Video");
		videoThread.start();

		if (audioStreamAdded) {
			audioThread = new Thread(this::recordAudio, "Screen Recorder - Audio");
			audioThread.start();
		}

		new Thread(this::finalizeWhenDone, "Screen Recorder - Finalizer").start();
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

	// ---------------------------------------------------------------------
	// Video capture
	// ---------------------------------------------------------------------

	private void recordScreen() {
		try {
			Robot robot = new Robot();

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

	// ---------------------------------------------------------------------
	// Audio capture (mic + optional system audio, mixed if both present)
	// ---------------------------------------------------------------------

	private void recordAudio() {
		byte[] rawMic = new byte[AUDIO_BUFFER_BYTES];
		byte[] rawSystem = new byte[AUDIO_BUFFER_BYTES];

		try {
			if (isMicrophoneEnabled) {
				micLine = openMic();
			}
			if (isSpeakerEnabled && systemAudioSource != null) {
				systemAudioSource.start(AUDIO_SAMPLE_RATE, AUDIO_CHANNELS);
			}

			while (isRecording) {
				awaitResumeIfPaused();
				if (!isRecording) {
					break;
				}

				short[] micSamples = null;
				short[] systemSamples = null;

				if (micLine != null) {
					int read = micLine.read(rawMic, 0, rawMic.length);
					if (read > 0) {
						micSamples = bytesToShorts(rawMic, read);
					}
				}

				if (isSpeakerEnabled && systemAudioSource != null) {
					int read = systemAudioSource.read(rawSystem, 0, rawSystem.length);
					if (read > 0) {
						systemSamples = bytesToShorts(rawSystem, read);
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
					long ts = currentTimestampNanos();
					synchronized (writerLock) {
						writer.encodeAudio(AUDIO_STREAM_INDEX, outSamples, ts, TimeUnit.NANOSECONDS);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error during audio capture: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (micLine != null) {
				micLine.stop();
				micLine.close();
			}
			if (systemAudioSource != null) {
				systemAudioSource.stop();
			}
		}
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

	private short[] mix(short[] a, short[] b) {
		int len = Math.min(a.length, b.length);
		short[] out = new short[len];
		for (int i = 0; i < len; i++) {
			int sum = a[i] + b[i];
			out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sum));
		}
		return out;
	}

	// ---------------------------------------------------------------------
	// Shared pause-aware clock, used by both the video and audio loops
	// ---------------------------------------------------------------------

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
		}

		NotificationUtil.notify("Video saved", outputFileName, new File(outputFileName), () -> {
			System.out.println("clicked");
		});
	}
}