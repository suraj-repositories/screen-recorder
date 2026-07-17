package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JWindow;

public class ControlFrame extends JWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final MainFrame mainFrame;
	private final SelectionFrame selectionFrame;

	private JComboBox<String> captureMode;
	private JRadioButton recordingRadio;
	private JRadioButton screenshotRadio;
	private JButton closeButton;

	public ControlFrame(SelectionFrame owner, MainFrame mainFrame, SelectionFrame selectionFrame) {

		super(owner);

		this.mainFrame = mainFrame;
		this.selectionFrame = selectionFrame;

		initializeUI();

		pack();

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		int x = (screen.width - getWidth()) / 2;
		int y = 20;

		setLocation(x, y);
		setAlwaysOnTop(true);
		setVisible(true);
	}

	private void initializeUI() {

		JPanel root = new JPanel(new BorderLayout());
		root.setBackground(new Color(36, 36, 36));
		root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(70, 70, 70), 1),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		left.setOpaque(false);

		Font font = new Font("Segoe UI", Font.PLAIN, 13);

		recordingRadio = createRadioButton("Screen Recording", true, font);
		screenshotRadio = createRadioButton("Screenshot", false, font);

		ButtonGroup group = new ButtonGroup();
		group.add(recordingRadio);
		group.add(screenshotRadio);

		captureMode = new JComboBox<>(new String[] { "Rectangle", "Entire Screen" });

		captureMode.setFont(font);
		captureMode.setPreferredSize(new Dimension(150, 30));

		left.add(recordingRadio);
		left.add(screenshotRadio);
		left.add(captureMode);

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		right.setOpaque(false);

		closeButton = new JButton("X");
		closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
		closeButton.setFocusable(false);
		closeButton.setPreferredSize(new Dimension(34, 30));

		closeButton.addActionListener(e -> {

			selectionFrame.closeSelection();
			setVisible(false);

			mainFrame.setVisible(true);
			mainFrame.toFront();
			mainFrame.requestFocus();
		});

		right.add(closeButton);

		root.add(left, BorderLayout.CENTER);
		root.add(right, BorderLayout.EAST);

		setContentPane(root);

		pack();

		setAlwaysOnTop(true);
	}

	private JRadioButton createRadioButton(String text, boolean selected, Font font) {

		JRadioButton radio = new JRadioButton(text);

		radio.setSelected(selected);
		radio.setOpaque(false);
		radio.setForeground(Color.WHITE);
		radio.setFont(font);
		radio.setFocusable(false);

		return radio;
	}

	/**
	 * Displays toolbar at given location.
	 */
	public void showToolbar(int x, int y) {
		setLocation(x, y);
		setVisible(true);
	}

	public boolean isRecordingMode() {
		return recordingRadio.isSelected();
	}

	public boolean isScreenshotMode() {
		return screenshotRadio.isSelected();
	}

	public String getCaptureMode() {
		return (String) captureMode.getSelectedItem();
	}

	public JButton getCloseButton() {
		return closeButton;
	}

}