package com.oranbyte.screenrec.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JWindow;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.constants.RecordingMode;
import com.oranbyte.screenrec.gui.components.ImageSwitch;
import com.oranbyte.screenrec.gui.components.RoundedBorder;

public class ControlFrame extends JWindow {

	private static final long serialVersionUID = 1L;

	private final MainFrame mainFrame;
	private final SelectionFrame selectionFrame;

	JComboBox<CaptureMode> captureModeComboBox;
	ImageSwitch recordingModeSwitch;
	private JRadioButton recordingRadio;
	private JRadioButton screenshotRadio;
	private JButton closeButton;

	public ControlFrame(SelectionFrame owner, MainFrame mainFrame, SelectionFrame selectionFrame) {

		super(owner);

		this.mainFrame = mainFrame;
		this.selectionFrame = selectionFrame;

		setBackground(new Color(0, 0, 0, 0));

		initializeUI();

		pack();

		setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		int x = (screen.width - getWidth()) / 2;
		int y = 20;

		setLocation(x, y);
		setAlwaysOnTop(true);
		setVisible(true);
	}

	private void initializeUI() {

		JPanel root = new JPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {

				Graphics2D g2 = (Graphics2D) g.create();

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				g2.setColor(AppColors.SURFACE);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

				super.paintComponent(g2);

				g2.dispose();
			}
		};

		root.setOpaque(false);
		root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));

		root.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(AppColors.BORDER, 10, 1),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		left.setOpaque(false);
		left.setAlignmentY(Component.CENTER_ALIGNMENT);

		recordingModeSwitch = new ImageSwitch(Icons.CAMERA.icon(24), Icons.VIDEO.icon(24));

		captureModeComboBox = new JComboBox<>(CaptureMode.values());
		captureModeComboBox.setPreferredSize(new Dimension(150, 30));

		captureModeComboBox.addActionListener(e -> {
			CaptureMode selectedMode = getCaptureMode();

			if (selectedMode != null) {
				setCaptureMode(selectedMode);
			}
		});

		left.add(recordingModeSwitch);
		left.add(captureModeComboBox);

		closeButton = new JButton("X");
		closeButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
		closeButton.setFocusable(false);
		closeButton.setPreferredSize(new Dimension(50, 30));
		closeButton.setAlignmentY(Component.CENTER_ALIGNMENT);

		closeButton.addActionListener(e -> {
			selectionFrame.closeSelection();
			setVisible(false);

			mainFrame.setVisible(true);
			mainFrame.toFront();
			mainFrame.requestFocus();
		});

		root.add(left);
		root.add(Box.createHorizontalGlue());
		root.add(closeButton);

		setContentPane(root);
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		setShape(new RoundRectangle2D.Double(0, 0, width, height, 10, 10));
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		setShape(new RoundRectangle2D.Double(0, 0, width, height, 10, 10));
	}

	public void showToolbar(int x, int y) {
		setLocation(x, y);
		setVisible(true);
	}

	public boolean isRecordingMode() {
		return recordingRadio != null && recordingRadio.isSelected();
	}

	public boolean isScreenshotMode() {
		return screenshotRadio != null && screenshotRadio.isSelected();
	}

	public CaptureMode getCaptureMode() {
		return (CaptureMode) captureModeComboBox.getSelectedItem();
	}

	public JButton getCloseButton() {
		return closeButton;
	}

	public void setCaptureMode(CaptureMode mode) {
		selectionFrame.drawPanel.setCaptureMode(mode);
		captureModeComboBox.setSelectedItem(mode);
	}

	public void setRecordingMode(RecordingMode mode) {
		recordingModeSwitch.setRecordingMode(mode);
	}
}