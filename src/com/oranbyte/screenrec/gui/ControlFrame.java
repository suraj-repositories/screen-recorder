package com.oranbyte.screenrec.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

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

	private RecordingState state = RecordingState.IDLE;

	public ControlFrame(SelectionFrame owner, MainFrame mainFrame, SelectionFrame selectionFrame) {

		super(owner);

		this.mainFrame = mainFrame;
		this.selectionFrame = selectionFrame;

		setBackground(new Color(0, 0, 0, 0));

		initializeUI();

		// Apply the initial (IDLE) visibility rules now that every control exists.
		setState(RecordingState.IDLE);

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

		// Start
		startButton = new ToolbarButton("Start", Icons.START);
		startButton.addActionListener(e -> setState(RecordingState.RECORDING));
		startButton.setBorder(null);

		// Pause
		pauseButton = new ToolbarButton(Icons.PAUSE);
		pauseButton.addActionListener(e -> setState(RecordingState.PAUSED));
		pauseButton.setBorder(null);

		// Resume
		playButton = new ToolbarButton(Icons.PLAY);
		playButton.addActionListener(e -> setState(RecordingState.RECORDING));
		playButton.setBorder(null);

		// Stop
		terminateButton = new ToolbarButton(Icons.STOP);
		terminateButton.setBorder(null);
		terminateButton.addActionListener(e -> {
			recordingTimeLabel.setText("00:00:00");
			setState(RecordingState.IDLE);
		});

		// Timer
		recordingTimeLabel = new JLabel("00:00:00");
		recordingTimeLabel.setForeground(AppColors.TEXT);
		recordingTimeLabel.setFont(AppConstant.APP_FONT.deriveFont(18f));

		// Mic icon
		micToggleButton = new ToolbarButton(Icons.MICROPHONE);
		micToggleButton.setBorder(null);

		// Desktop audio icon
		speakerToggleButton = new ToolbarButton(Icons.VOLUME);
		speakerToggleButton.setBorder(null);

		recordingControlsPanel.add(startButton);
		recordingControlsPanel.add(Box.createHorizontalStrut(12));

		recordingControlsPanel.add(pauseButton);
		recordingControlsPanel.add(Box.createHorizontalStrut(12));

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

	/**
	 * Single source of truth for what's visible/enabled at each step of the capture
	 * flow:
	 * <ul>
	 * <li>IDLE - only the mode selectors (recording mode switch + capture mode
	 * combo box) are shown; the recording controls panel is hidden entirely.</li>
	 * <li>SELECTING - mode selectors hidden, recording panel shown with only the
	 * Start button visible and disabled (selection isn't finished yet).</li>
	 * <li>READY - same as SELECTING but Start is enabled (selection finished).</li>
	 * <li>RECORDING - Start/Resume hidden, Pause + Terminate + timer + mic/ speaker
	 * icons shown; mode selectors stay hidden and locked.</li>
	 * <li>PAUSED - Pause replaced by Resume, Terminate + timer + mic/speaker stay
	 * visible; mode selectors stay hidden and locked.</li>
	 * </ul>
	 */
	public void setState(RecordingState newState) {
		this.state = newState;

		System.out.println(newState.toString());

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

}