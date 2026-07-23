package com.oranbyte.screenrec.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.constants.RecordingMode;
import com.oranbyte.screenrec.constants.RecordingState;
import com.oranbyte.screenrec.gui.components.ImageSwitch;
import com.oranbyte.screenrec.gui.components.RoundedBorder;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.ToolbarComboBox;
import com.oranbyte.screenrec.recorder.ScreenRecorder;

public class ControlFrame extends JWindow {

	private static final long serialVersionUID = 1L;

	private final MainFrame mainFrame;
	private final SelectionFrame selectionFrame;

	private JPanel root;

	private JPanel modeControlsPanel;
	ToolbarComboBox<CaptureMode> captureModeComboBox;
	ImageSwitch recordingModeSwitch;

	private JPanel recordingControlsPanel;
	private ToolbarButton startButton;
	private ToolbarButton pauseButton;
	private ToolbarButton playButton;
	private ToolbarButton terminateButton;
	private JLabel recordingTimeLabel;
	private ToolbarButton micToggleButton;
	private ToolbarButton speakerToggleButton;
	private ToolbarButton closeButton;
	private final int CONTROL_PADDING = 5;

	private RecordingState state = RecordingState.IDLE;
	private ScreenRecorder recorder;

	private Timer recordingTimer;
	private int elapsedSeconds = 0;

	private Rectangle preRecordingLocation;

	public ControlFrame(SelectionFrame owner, MainFrame mainFrame, SelectionFrame selectionFrame) {

		super(mainFrame);

		this.mainFrame = mainFrame;
		this.selectionFrame = selectionFrame;

		setBackground(new Color(0, 0, 0, 0));

		initializeUI();

		setState(RecordingState.IDLE);

		pack();

		setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		int x = (screen.width - getWidth()) / 2;
		int y = 20;

		setLocation(x, y);
		setAlwaysOnTop(true);
		setVisible(true);

		initializeTimer();
	}

	private void initializeTimer() {
		recordingTimer = new Timer(1000, e -> {
			elapsedSeconds++;
			updateElapsedLabel();
		});
	}

	private void updateElapsedLabel() {
		int h = elapsedSeconds / 3600;
		int m = (elapsedSeconds % 3600) / 60;
		int s = elapsedSeconds % 60;
		recordingTimeLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
	}

	private void initializeUI() {

		root = new JPanel() {

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

		buildRecordingControlsPanel();
		buildModeControlsPanel();

		closeButton = new ToolbarButton(Icons.CLOSE);
		closeButton.setHorizontalAlignment(SwingConstants.CENTER);
		closeButton.setPreferredSize(new Dimension(42, 42));
		closeButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);

		closeButton.addActionListener(e -> {
			selectionFrame.closeSelection();
			setVisible(false);

			mainFrame.setVisible(true);
			mainFrame.toFront();
			mainFrame.requestFocus();
		});

		root.add(recordingControlsPanel);
		root.add(modeControlsPanel);
		root.add(Box.createHorizontalGlue());
		root.add(Box.createHorizontalStrut(16));
		root.add(closeButton);

		setContentPane(root);
	}

	private void buildModeControlsPanel() {

		modeControlsPanel = new JPanel();
		modeControlsPanel.setOpaque(false);
		modeControlsPanel.setLayout(new BoxLayout(modeControlsPanel, BoxLayout.X_AXIS));
		modeControlsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

		recordingModeSwitch = new ImageSwitch(Icons.CAMERA.icon(24), Icons.VIDEO.icon(24));

		captureModeComboBox = new ToolbarComboBox<>(CaptureMode.values());

		captureModeComboBox.addActionListener(e -> {
			CaptureMode selectedMode = getCaptureMode();

			if (selectedMode != null) {
				setCaptureMode(selectedMode);
			}
		});

		modeControlsPanel.add(recordingModeSwitch);
		modeControlsPanel.add(Box.createHorizontalStrut(12));
		modeControlsPanel.add(captureModeComboBox);
	}

	private void buildRecordingControlsPanel() {

		recordingControlsPanel = new JPanel();
		recordingControlsPanel.setOpaque(false);
		recordingControlsPanel.setLayout(new BoxLayout(recordingControlsPanel, BoxLayout.X_AXIS));
		recordingControlsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

		startButton = new ToolbarButton("Start", Icons.START);
		startButton.addActionListener(e -> {
			setState(RecordingState.RECORDING);
			startRecording();
		});
		startButton.setBorder(null);

		pauseButton = new ToolbarButton(Icons.PAUSE);
		pauseButton.addActionListener(e -> {
			setState(RecordingState.PAUSED);
			pauseRecording();
		});
		pauseButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);
		pauseButton.setBorder(null);

		playButton = new ToolbarButton(Icons.PLAY);
		playButton.addActionListener(e -> {
			setState(RecordingState.RECORDING);
			resumeRecording();
		});
		playButton.setBorder(null);
		playButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);

		terminateButton = new ToolbarButton(Icons.STOP);
		terminateButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);
		terminateButton.setBorder(null);
		terminateButton.addActionListener(e -> {
			setState(RecordingState.IDLE);
			stopRecording();
		});

		recordingTimeLabel = new JLabel("00:00:00");
		recordingTimeLabel.setForeground(AppColors.TEXT);
		recordingTimeLabel.setFont(AppConstant.APP_FONT.deriveFont(18f));

		micToggleButton = new ToolbarButton(Icons.MICROPHONE);
		micToggleButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);
		micToggleButton.setBorder(null);

		speakerToggleButton = new ToolbarButton(Icons.VOLUME);
		speakerToggleButton.setBorder(null);
		speakerToggleButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);

		recordingControlsPanel.add(startButton);

		recordingControlsPanel.add(pauseButton);

		recordingControlsPanel.add(playButton);
		recordingControlsPanel.add(Box.createHorizontalStrut(12));

		recordingControlsPanel.add(terminateButton);
		recordingControlsPanel.add(Box.createHorizontalStrut(16));

		recordingControlsPanel.add(recordingTimeLabel);
		recordingControlsPanel.add(Box.createHorizontalStrut(16));

		recordingControlsPanel.add(micToggleButton);
		recordingControlsPanel.add(Box.createHorizontalStrut(12));

		recordingControlsPanel.add(speakerToggleButton);
	}

	public void setState(RecordingState newState) {
		this.state = newState;

		boolean idle = newState == RecordingState.IDLE;
		boolean selecting = newState == RecordingState.SELECTING;
		boolean ready = newState == RecordingState.READY;
		boolean recording = newState == RecordingState.RECORDING;
		boolean paused = newState == RecordingState.PAUSED;

		modeControlsPanel.setVisible(idle);
		recordingControlsPanel.setVisible(!idle);

		startButton.setVisible(selecting || ready);
		startButton.setEnabled(ready);

		pauseButton.setVisible(recording);
		playButton.setVisible(paused);
		terminateButton.setVisible(recording || paused);

		recordingTimeLabel.setVisible(recording || paused || selecting || ready);
		micToggleButton.setVisible(recording || paused || selecting || ready);
		speakerToggleButton.setVisible(recording || paused || selecting || ready);

		boolean locked = recording || paused;
		captureModeComboBox.setEnabled(!locked);
		recordingModeSwitch.setEnabled(!locked);

		if (selectionFrame != null && selectionFrame.drawPanel != null) {
			selectionFrame.drawPanel.setRecordingActive(locked);
		}

		root.revalidate();
		root.repaint();

		pack();
		setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
	}

	public RecordingState getState() {
		return state;
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

	public RecordingMode getRecordingMode() {
		return recordingModeSwitch.getRecordingMode();
	}

	public void startRecording() {
		if (selectionFrame == null || selectionFrame.drawPanel == null
				|| selectionFrame.drawPanel.selectedRectangle == null) {
			JOptionPane.showMessageDialog(this, "Please create a selection first.");
			return;
		}

		Rectangle captureArea = ensureEvenDimensions(selectionFrame.drawPanel.selectedRectangle);
		if (captureArea.width <= 0 || captureArea.height <= 0) {

			JOptionPane.showMessageDialog(this, "Please select a valid recording area.");
			return;
		}

		avoidOverlapWithCaptureArea(captureArea);

		selectionFrame.setVisible(false);

		recorder = new ScreenRecorder(captureArea);
		recorder.start();

		elapsedSeconds = 0;
		updateElapsedLabel();
		recordingTimer.start();

		toFront();
		requestFocus();
	}

	public void pauseRecording() {
		recordingTimer.stop();

		if (recorder != null) {
			recorder.pause();
		}
	}

	public void resumeRecording() {
		recordingTimer.start();

		if (recorder != null)
			recorder.resume();
	}

	public void stopRecording() {

		recordingTimer.stop();
		elapsedSeconds = 0;
		updateElapsedLabel();

		if (recorder != null) {
			recorder.stop();
			recorder = null;
		}

		restoreLocationIfMoved();

		if (selectionFrame != null) {
			selectionFrame.dispose();
		}
	}

	private void avoidOverlapWithCaptureArea(Rectangle captureArea) {

		Rectangle myBounds = getBounds();

		if (!myBounds.intersects(captureArea)) {
			preRecordingLocation = null;
			return;
		}

		preRecordingLocation = myBounds;

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int margin = 10;
		int newX = myBounds.x;
		int newY;

		if (captureArea.y - myBounds.height - margin >= 0) {
			newY = captureArea.y - myBounds.height - margin;
		} else if (captureArea.y + captureArea.height + myBounds.height + margin <= screen.height) {
			newY = captureArea.y + captureArea.height + margin;
		} else {
			newY = 0;
			if (captureArea.x <= 0 && captureArea.x + captureArea.width >= screen.width) {
				newX = Math.max(0, screen.width - myBounds.width - margin);
			}
		}

		setLocation(newX, newY);
	}

	private void restoreLocationIfMoved() {
		if (preRecordingLocation != null) {
			setLocation(preRecordingLocation.x, preRecordingLocation.y);
			preRecordingLocation = null;
		}
	}

	public static Rectangle ensureEvenDimensions(Rectangle rect) {

		if (rect == null) {
			return null;
		}

		int width = rect.width;
		int height = rect.height;

		if ((width & 1) == 1) {
			width--;
		}

		if ((height & 1) == 1) {
			height--;
		}

		return new Rectangle(rect.x, rect.y, width, height);
	}

}