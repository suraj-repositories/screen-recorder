package com.oranbyte.screenrec;

import java.io.File;

import javax.swing.SwingUtilities;
import com.oranbyte.screenrec.gui.MainFrame;
import com.oranbyte.screenrec.util.NotificationUtil;

import javafx.embed.swing.JFXPanel;

public class Main {

	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale", "1.0");
		
		cleanup();

		new JFXPanel();
		SwingUtilities.invokeLater(() -> {
			new MainFrame();
		});

		NotificationUtil.initializeActions();
	}
	
	
	private static void cleanup() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		    File tempDir = new File(System.getProperty("java.io.tmpdir"), "screenrec_thumbs");
		    if (tempDir.exists() && tempDir.isDirectory()) {
		        File[] files = tempDir.listFiles();
		        if (files != null) {
		            for (File file : files) {
		                file.delete();
		            }
		        }
		        tempDir.delete();
		    }
		}));
	}
}