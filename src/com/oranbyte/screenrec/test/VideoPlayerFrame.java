package com.oranbyte.screenrec.test;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.gui.VideoPlayerPanel;

public class VideoPlayerFrame extends JFrame {

    private final VideoPlayerPanel panel;

    public VideoPlayerFrame(String videoPath) {
        super("Video Player");

        panel = new VideoPlayerPanel();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setContentPane(panel);

        setSize(1000, 650);
        setLocationRelativeTo(null);

        panel.open(videoPath);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String videoPath =
                    "C:\\Users\\Shubham\\Videos\\REACT AUTH\\2- React User Login and Authentication with Axios_720p.mp4";

            VideoPlayerFrame frame = new VideoPlayerFrame(videoPath);
            frame.setVisible(true);
        });
    }
}