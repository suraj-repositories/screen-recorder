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

import javax.swing.JWindow;

import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.RecordingMode;

public class SelectionFrame extends JWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	DrawSelectRectangle drawPanel;
	private ControlFrame controlFrame;

	public SelectionFrame() {
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

		drawPanel = new DrawSelectRectangle(this, screenImage);

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
		drawPanel.setControlFrame(controlFrame);
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

	public void setCaptureMode(CaptureMode mode) {
		controlFrame.captureModeComboBox.setSelectedItem(mode);
	}

	public CaptureMode getCaptureMode() {
		if (controlFrame == null) {
			return CaptureMode.RECTANGLE;
		}
		return controlFrame.getCaptureMode();
	}

	public RecordingMode getRecordingMode() {
		return controlFrame == null ? RecordingMode.SCREENSHOT : controlFrame.getRecordingMode();
	}

	public void setRecordingMode(RecordingMode mode) {
		controlFrame.recordingModeSwitch.setRecordingMode(mode);
	}

}