package com.oranbyte.screenrec.recorder;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import com.oranbyte.screenrec.constants.AppConstant;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class ScreenRecorder {

	private final Rectangle captureArea;
	private final String outputFileName;

	private volatile boolean isRecording;
	private volatile boolean isPaused;

	private final Object pauseLock = new Object();

	public ScreenRecorder(Rectangle captureArea) {
		this.captureArea = captureArea;
		this.outputFileName = AppConstant.SAVE_LOCATION + "\\orange_" + System.currentTimeMillis() + ".mp4";
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

		IMediaWriter writer = ToolFactory.makeWriter(outputFileName);
		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, captureArea.width, captureArea.height);

		long startTime = System.nanoTime();

		try {
			Robot robot = new Robot();

			while (isRecording) {

				synchronized (pauseLock) {
					while (isPaused && isRecording) {
						pauseLock.wait();
					}
				}

				if (!isRecording) {
					break;
				}

				BufferedImage image = robot.createScreenCapture(captureArea);

				BufferedImage bgrImage = new BufferedImage(image.getWidth(), image.getHeight(),
						BufferedImage.TYPE_3BYTE_BGR);

				Graphics2D g = bgrImage.createGraphics();
				g.drawImage(image, 0, 0, null);
				g.dispose();

				writer.encodeVideo(0, bgrImage, System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

				Thread.sleep(20);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.close();
			VideoUtils.showSaveDialog(outputFileName);
		}
	}
}