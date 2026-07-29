package com.oranbyte.screenrec.test;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.gui.ImageViewerPanel;

public class PlayerFrame extends JFrame {

	public PlayerFrame(File file) throws IOException {

		setTitle("Screen Recorder Player");
		setSize(900, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		VideoPlayerPanel player = new VideoPlayerPanel();
//		add(player);
//		player.play(file);

		BufferedImage image = ImageIO
				.read(new File("C:\\Users\\Shubham\\Pictures\\Screenshots\\Screenshot 2026-02-28 143820.png"));

		ImageViewerPanel imageViewer = new ImageViewerPanel();
		imageViewer.setImage(image);

		add(imageViewer, BorderLayout.CENTER);

	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {

			PlayerFrame frame;
			try {
				frame = new PlayerFrame(new File("C:\\Users\\Shubham\\Videos\\Screen Recordings\\fixed.mp4"));
				frame.setVisible(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});

	}
}