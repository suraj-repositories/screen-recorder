package com.oranbyte.screenrec.recorder;

import javax.swing.JOptionPane;

import com.xuggle.xuggler.IContainer;

public class VideoUtils {
    public static void showSaveDialog(String filePath) {
        IContainer container = IContainer.make();
        int result = container.open(filePath, IContainer.Type.READ, null);

        if (result < 0) {
            JOptionPane.showMessageDialog(null, "Failed to open saved video.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null,
                    "<html><b>Recording Saved!</b><br/>File: " + filePath +
                            "<br/>Size: " + container.getFileSize() + " bytes" +
                            "<br/>Duration: " + container.getDuration() / 1_000_000 + " ms" +
                            "<br/>Bitrate: " + container.getBitRate() + "</html>",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
