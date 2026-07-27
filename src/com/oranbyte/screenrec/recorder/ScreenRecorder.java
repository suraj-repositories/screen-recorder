package com.oranbyte.screenrec.recorder;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.util.CursorUtils;
import com.oranbyte.screenrec.util.NotificationUtil;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class ScreenRecorder {
	private final Rectangle captureArea;
	private final String outputFileName;
	private volatile boolean isRecording;
	private volatile boolean isPaused;
	private final Object pauseLock = new Object();

	private static volatile int TARGET_FPS = 50;

	public ScreenRecorder(Rectangle captureArea) {

		int width = captureArea.width % 2 == 0 ? captureArea.width : captureArea.width - 1;
		int height = captureArea.height % 2 == 0 ? captureArea.height : captureArea.height - 1;

		this.captureArea = new Rectangle(captureArea.x, captureArea.y, width, height);

		File saveDir = new File(AppConstant.SAVE_LOCATION);
		if (!saveDir.exists()) {
			saveDir.mkdirs();
		}

		this.outputFileName = AppConstant.SAVE_LOCATION + File.separator + "orange_" + System.currentTimeMillis()
				+ ".mp4";
	}

	public void start() {
		if (isRecording) {
			return;
		}
		isRecording = true;
		isPaused = false;
		new Thread(this::recordScreen, "Screen Recorder").start();
	}

	public void stop() {
		isRecording = false;
		resume();
	}

	public void pause() {
		if (isRecording) {
			isPaused = true;
		}
	}

	public void resume() {
		synchronized (pauseLock) {
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

	private void recordScreen() {
		IMediaWriter writer = null;
		int framesEncoded = 0;

		try {
			writer = ToolFactory.makeWriter(outputFileName);
			writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, captureArea.width, captureArea.height);

			Robot robot = new Robot();
			long startTime = System.nanoTime();
			long totalPausedTime = 0;

			while (isRecording) {
				if (isPaused) {
					long pauseStart = System.nanoTime();
					synchronized (pauseLock) {
						while (isPaused && isRecording) {
							pauseLock.wait();
						}
					}
					long pauseEnd = System.nanoTime();
					totalPausedTime += (pauseEnd - pauseStart);
				}

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

				long frameTimeStamp = (System.nanoTime() - startTime) - totalPausedTime;
				writer.encodeVideo(0, bgrImage, frameTimeStamp, TimeUnit.NANOSECONDS);
				framesEncoded++;

				long timeTaken = System.currentTimeMillis() - frameStart;
				long sleepTime = frameIntervalMs - timeTaken;
				if (sleepTime > 0) {
					Thread.sleep(sleepTime);
				}
			}
		} catch (Exception e) {
			System.err.println("Error during recording: " + e.getMessage());
			e.printStackTrace();
		} finally {

			if (writer != null) {
				try {
					if (framesEncoded > 0) {
						writer.close();
					} else {
						System.err.println("No frames were recorded; skipping writer.close()");
					}
				} catch (Exception e) {
					System.err.println("Failed to finalize video trailer: " + e.getMessage());
				}
			}

			NotificationUtil.notify("Video saved", outputFileName, new File(outputFileName), () -> {

				try {
					System.out.println("clicked");
					Desktop.getDesktop().open(new File(outputFileName));
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});

			System.out.println(outputFileName);
			new Thread(() -> {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
//				System.exit(0);
			}, "Shutdown Thread").start();
		}
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
}