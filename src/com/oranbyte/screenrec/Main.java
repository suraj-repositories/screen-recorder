package com.oranbyte.screenrec;

import javax.swing.SwingUtilities;
import com.oranbyte.screenrec.gui.MainFrame;
import com.oranbyte.screenrec.util.NotificationUtil;

import javafx.embed.swing.JFXPanel;

public class Main {

	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale", "1.0");

		new JFXPanel();
		SwingUtilities.invokeLater(() -> {
			new MainFrame();
		});

		NotificationUtil.initializeActions();
	}
}