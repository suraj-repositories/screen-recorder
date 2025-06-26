package com.oranbyte.screenrec.recorder;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

public class ScreenRecorder {
    private final Rectangle captureArea;
    private final String outputFileName;
    private boolean isRecording;

    public ScreenRecorder(Rectangle captureArea) {
        this.captureArea = captureArea;
        this.outputFileName = "C:\\Users\\Shubham\\Desktop\\orange_" + System.currentTimeMillis() + ".mp4";
    }

    public void start() {
        isRecording = true;
        new Thread(this::recordScreen).start();
    }

    public void stop() {
        isRecording = false;
    }

    private void recordScreen() {
        IMediaWriter writer = ToolFactory.makeWriter(outputFileName);
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, captureArea.width, captureArea.height);

        long startTime = System.nanoTime();
        try {
            Robot robot = new Robot();
            while (isRecording) {
                BufferedImage image = robot.createScreenCapture(captureArea);
                BufferedImage bgrImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D g = bgrImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();

                writer.encodeVideo(0, bgrImage, System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                Thread.sleep(20);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        writer.close();
        VideoUtils.showSaveDialog(outputFileName);
    }
}
