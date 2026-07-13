package com.oranbyte.screenrec;

import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.gui.MainFrame;

public class Main {
	public static void main(String[] args) {
		System.setProperty("sun.java2d.uiScale", "1.0");
		SwingUtilities.invokeLater(() -> new MainFrame());
	}
}
