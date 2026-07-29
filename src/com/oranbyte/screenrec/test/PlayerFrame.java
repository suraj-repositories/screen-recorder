package com.oranbyte.screenrec.test;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.gui.VideoPlayerPanel;

public class PlayerFrame extends JFrame {

	public PlayerFrame(File file) {

		setTitle("Screen Recorder Player");
		setSize(900, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		VideoPlayerPanel player = new VideoPlayerPanel();
		add(player);
		player.play(file);

	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {

			PlayerFrame frame = new PlayerFrame(new File("C:\\Users\\Shubham\\Videos\\Screen Recordings\\fixed.mp4"));

			frame.setVisible(true);

		});

	}
}