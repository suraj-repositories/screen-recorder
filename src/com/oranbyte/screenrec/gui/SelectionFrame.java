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

import com.oranbyte.screenrec.constants.CaptureMode;

public class SelectionFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	DrawSelectRectangle drawPanel;
	private ControlFrame controlFrame;

	public SelectionFrame() {
		setUndecorated(true);
		setAlwaysOnTop(true);
		setBackground(new Color(0, 0, 0, 0));

		getRootPane().setOpaque(false);
		getContentPane().setBackground(new Color(0, 0, 0, 0));

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screenSize);
		setLocation(0, 0);
		setLayout(new BorderLayout());

		BufferedImage screenImage = null;

		try {
			GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
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
		return drawPanel.getSelectedRectangle();
	}

	public void refreshScreen() {

		try {
			GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDefaultConfiguration();
			Rectangle bounds = gc.getBounds();
			BufferedImage image = new Robot(gc.getDevice()).createScreenCapture(bounds);
			drawPanel.setScreenImage(image);
			drawPanel.repaint();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void closeSelection() {
		setVisible(false);
	}

	public void activate(MainFrame mainFrame) {
		refreshScreen();
		setVisible(true);
		showControlFrame(mainFrame);
	}

	private void showControlFrame(MainFrame mainFrame) {

		if (controlFrame == null) {
			controlFrame = new ControlFrame(this, mainFrame, this);
		}

		controlFrame.setVisible(true);
		controlFrame.toFront();
		controlFrame.requestFocus();
	}

	public void disposeControlFrame() {

		if (controlFrame != null) {
			controlFrame.dispose();
			controlFrame = null;
		}
	}

	@Override
	public void dispose() {
		disposeControlFrame();
		super.dispose();
	}

	public void setCaptureMode(CaptureMode captureMode) {
		controlFrame.captureModeComboBox.setSelectedItem(captureMode);
	}
}