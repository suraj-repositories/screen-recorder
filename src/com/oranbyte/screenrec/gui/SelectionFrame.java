package com.oranbyte.screenrec.gui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class SelectionFrame extends JFrame {

	DrawSelectRectangle drawPanel;
	
	public SelectionFrame() {
		setUndecorated(true);
		setAlwaysOnTop(true);
		setBackground(new Color(0, 0, 0, 0));

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screenSize);
		setLocation(0, 0);
		setLayout(new BorderLayout());

		
		BufferedImage screenImage = null;
		try {
			GraphicsConfiguration gc = GraphicsEnvironment
				    .getLocalGraphicsEnvironment()
				    .getDefaultScreenDevice()
				    .getDefaultConfiguration();

				Rectangle screenBounds = gc.getBounds();

			screenImage = new Robot(gc.getDevice()).createScreenCapture(screenBounds);
		} catch (HeadlessException | AWTException e) {
		    e.printStackTrace();
		}

		drawPanel = new DrawSelectRectangle(screenImage);

		drawPanel.setOpaque(false);
		drawPanel.setBackground(new Color(0, 0, 0, 0));
		getContentPane().add(drawPanel, BorderLayout.CENTER);

		setVisible(true);
	}

	public Rectangle getCaptureBounds() {
		Rectangle bounds = getBounds();
		bounds.width = (int) (Math.ceil(bounds.width / 2.0) * 2);
		bounds.height = (int) (Math.ceil(bounds.height / 2.0) * 2);
		return bounds;
	}
}
