package com.oranbyte.screenrec.gui;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.constants.RecordingMode;
import com.oranbyte.screenrec.constants.RecordingState;
import com.oranbyte.screenrec.gui.components.CountdownOverlay;
import com.oranbyte.screenrec.gui.components.ImageSwitch;
import com.oranbyte.screenrec.gui.components.RecordingBorderOverlay;
import com.oranbyte.screenrec.gui.components.RoundedBorder;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.ToolbarComboBox;
import com.oranbyte.screenrec.recorder.ScreenRecorder;
import com.oranbyte.screenrec.util.NotificationUtil;

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
//	private ToolbarButton dropperButton;
	private final int CONTROL_PADDING = 5;

	private RecordingState state = RecordingState.IDLE;
	private ScreenRecorder recorder;

	private Timer recordingTimer;
	private int elapsedSeconds = 0;
	private boolean isMicrophoneEnabled = false;
	private boolean isSpeakerEnabled = true;

	private Rectangle preRecordingLocation;
	private RecordingBorderOverlay recordingBorderOverlay;

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

		com.oranbyte.screenrec.util.ScreenCaptureExclusion.excludeFromCapture(this);

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
		closeButton.setVerticalAlignment(SwingConstants.CENTER);
		closeButton.setHorizontalTextPosition(SwingConstants.CENTER);
		closeButton.setVerticalTextPosition(SwingConstants.CENTER);
		closeButton.setPreferredSize(new Dimension(42, 42));
		closeButton.setMinimumSize(new Dimension(42, 42));
		closeButton.setMaximumSize(new Dimension(42, 42));
		closeButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);

		closeButton.addActionListener(e -> {
			if (state == RecordingState.RECORDING || state == RecordingState.PAUSED) {
				cancelRecording();
			}

			selectionFrame.closeSelection();
			setVisible(false);
			if (recordingBorderOverlay != null) {
				recordingBorderOverlay.setVisible(false);
			}

			mainFrame.showNoContentPanel();
			mainFrame.setSize(mainFrame.defaultDimention);
			mainFrame.setLocationRelativeTo(null);
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
		recordingModeSwitch.addChangeListener(e -> {
			ImageSwitch source = (ImageSwitch) e.getSource();
			RecordingMode selectedMode = source.getRecordingMode();

			if (selectedMode != null) {
				setRecordingMode(selectedMode);
			}
		});

		captureModeComboBox = new ToolbarComboBox<>(CaptureMode.values());

		captureModeComboBox.addActionListener(e -> {
			CaptureMode selectedMode = getCaptureMode();

			if (selectedMode != null) {
				setCaptureMode(selectedMode);
			}
		});

//		dropperButton = new ToolbarButton(Icons.DROPPER);
//		dropperButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);
//		dropperButton.setHorizontalAlignment(SwingConstants.CENTER); 

		modeControlsPanel.add(recordingModeSwitch);
		modeControlsPanel.add(Box.createHorizontalStrut(12));
		modeControlsPanel.add(captureModeComboBox);
//		modeControlsPanel.add(Box.createHorizontalStrut(12));
//		modeControlsPanel.add(dropperButton);
	}

	private void buildRecordingControlsPanel() {

		recordingControlsPanel = new JPanel();
		recordingControlsPanel.setOpaque(false);
		recordingControlsPanel.setLayout(new BoxLayout(recordingControlsPanel, BoxLayout.X_AXIS));
		recordingControlsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

		startButton = new ToolbarButton("Start", Icons.START);
		startButton.addActionListener(e -> {
			setState(RecordingState.RECORDING);

			setButtonsEnabled(false);
			startRecording();
		});

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
		micToggleButton.setAllowed(isMicrophoneEnabled);
		micToggleButton.addActionListener(e -> {

			isMicrophoneEnabled = !isMicrophoneEnabled;
			micToggleButton.setAllowed(isMicrophoneEnabled);
			if (recorder != null) {
				recorder.setMicrophoneEnabled(isMicrophoneEnabled);
			}
		});

		speakerToggleButton = new ToolbarButton(Icons.VOLUME);
		speakerToggleButton.setBorder(null);
		speakerToggleButton.setPadding(CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING, CONTROL_PADDING);
		speakerToggleButton.addActionListener(e -> {

			isSpeakerEnabled = !isSpeakerEnabled;
			speakerToggleButton.setAllowed(isSpeakerEnabled);
			if (recorder != null) {
				recorder.setSpeakerEnabled(isSpeakerEnabled);
			}
		});

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

	public void setState(@SuppressWarnings("exports") RecordingState newState) {
		  
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> setState(newState));
			return;
		}

		this.state = newState;

		boolean idle = newState == RecordingState.IDLE;
		boolean selecting = newState == RecordingState.SELECTING;
		boolean ready = newState == RecordingState.READY;
		boolean recording = newState == RecordingState.RECORDING;
		boolean paused = newState == RecordingState.PAUSED;
		boolean locked = recording || paused;

		if (closeButton != null) {
			if (locked) {
				closeButton.setIcon(Icons.TRASH.icon(24));
			} else {
				closeButton.setIcon(Icons.CLOSE.icon(ToolbarButton.DEFAULT_ICON_SIZE));
			}
		}

		RecordingMode rMode = getRecordingMode();
  
		if (rMode == RecordingMode.VIDEO) {
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
			captureModeComboBox.setEnabled(!locked);
			recordingModeSwitch.setEnabled(!locked);

			if (selectionFrame != null && selectionFrame.drawPanel != null) {
				selectionFrame.drawPanel.setRecordingActive(locked);
			}
		} else if (rMode == RecordingMode.SCREENSHOT) {
			modeControlsPanel.setVisible(true);
			recordingControlsPanel.setVisible(false);

			if (selectionFrame != null && selectionFrame.drawPanel != null) {
				selectionFrame.drawPanel.setRecordingActive(false);

				if (ready || locked) {
					String screenshot = takeScreenshot();
					if (screenshot != null) {
						try {
							showScreenshot(screenshot);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

		}

		root.revalidate();
		root.repaint();
		pack();
		setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
	}

	public void showScreenshot(String screenshot) throws IOException {
		File file = new File(screenshot);
		BufferedImage image = ImageIO.read(file);

		ImageViewerPanel imageViewer = new ImageViewerPanel();
		imageViewer.setImage(image);

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		int maxWidth = (int) (screen.width * 0.85);
		int maxHeight = (int) (screen.height * 0.85);

		double scaleX = (double) maxWidth / image.getWidth();
		double scaleY = (double) maxHeight / image.getHeight();

		double initialZoom = Math.min(1.0, Math.min(scaleX, scaleY));

		imageViewer.setZoom(initialZoom);

		int viewerWidth = (int) (image.getWidth() * initialZoom);
		int viewerHeight = (int) (image.getHeight() * initialZoom);

		viewerWidth = Math.min(viewerWidth + 20, maxWidth);
		viewerHeight = Math.min(viewerHeight + 40, maxHeight);

		mainFrame.setActionButtons(file);
		mainFrame.setPanelContent(imageViewer);
		mainFrame.setPreferredSize(new Dimension(viewerWidth, viewerHeight));
		mainFrame.pack();
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	@SuppressWarnings("exports")
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

	@SuppressWarnings("exports")
	public CaptureMode getCaptureMode() {
		return (CaptureMode) captureModeComboBox.getSelectedItem();
	}

	@SuppressWarnings("exports")
	public JButton getCloseButton() {
		return closeButton;
	}

	public void setCaptureMode(@SuppressWarnings("exports") CaptureMode mode) {
		selectionFrame.drawPanel.setCaptureMode(mode);
		captureModeComboBox.setSelectedItem(mode);
	}

	public void setRecordingMode(@SuppressWarnings("exports") RecordingMode mode) {
		selectionFrame.drawPanel.setRecordingMode(mode);
		recordingModeSwitch.setRecordingMode(mode);
	}

	@SuppressWarnings("exports")
	public RecordingMode getRecordingMode() {
		return recordingModeSwitch.getRecordingMode();
	}

	public String takeScreenshot() {

		if (selectionFrame == null || selectionFrame.drawPanel == null
				|| selectionFrame.drawPanel.selectedRectangle == null) {
			JOptionPane.showMessageDialog(this, "Please create a selection first.");
			return null;
		}

		Rectangle captureArea = ensureEvenDimensions(selectionFrame.drawPanel.selectedRectangle);
		if (captureArea.width <= 0 || captureArea.height <= 0) {
			JOptionPane.showMessageDialog(this, "Please select a valid capture area.");
			return null;
		}

		boolean wasControlFrameVisible = isVisible();

		selectionFrame.setVisible(false);
		setVisible(false);

		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			restoreAfterScreenshot(wasControlFrameVisible);
			return null;
		}

		BufferedImage image;
		try {
			Robot robot = new Robot();
			image = robot.createScreenCapture(captureArea);
		} catch (AWTException e) {
			JOptionPane.showMessageDialog(this, "Failed to capture screenshot: " + e.getMessage());
			restoreAfterScreenshot(wasControlFrameVisible);
			return null;
		}

		File saveDir = new File(AppConstant.SAVE_LOCATION_SCREENSHOT);
		if (!saveDir.exists() && !saveDir.mkdirs()) {
			JOptionPane.showMessageDialog(this, "Failed to create save directory.");
			restoreAfterScreenshot(wasControlFrameVisible);
			return null;
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss");
		String timestamp = LocalDateTime.now().format(formatter);
		String outputFileName = AppConstant.SAVE_LOCATION_SCREENSHOT + File.separator + "Screenshot " + timestamp + ".png";

		try {
			ImageIO.write(image, "png", new File(outputFileName));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Failed to save screenshot: " + e.getMessage());
			restoreAfterScreenshot(wasControlFrameVisible);
			return null;
		}
		NotificationUtil.notify("Screenshot saved", outputFileName, new File(outputFileName), () -> {
			System.out.println("clicked");
		});
		return outputFileName;
	}

	private void restoreAfterScreenshot(boolean wasControlFrameVisible) {
		if (wasControlFrameVisible) {
			setVisible(true);
			toFront();
			requestFocus();
		}
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

		selectionFrame.setVisible(false);

		CountdownOverlay overlay = new CountdownOverlay(captureArea);

		overlay.startCountdown(() -> {

			recordingBorderOverlay = new RecordingBorderOverlay(captureArea);
			recordingBorderOverlay.setVisible(true);

			setButtonsEnabled(true);

			recorder = new ScreenRecorder(mainFrame, captureArea, true, false);
			recorder.start();

			elapsedSeconds = 0;
			updateElapsedLabel();
			recordingTimer.start();

			toFront();
			requestFocus();
		});
	}

	public void setButtonsEnabled(boolean enabled) {
		if (pauseButton != null) {
			pauseButton.setEnabled(enabled);
		}
		if (playButton != null) {
			playButton.setEnabled(enabled);
		}
		if (terminateButton != null) {
			terminateButton.setEnabled(enabled);
		}
		if (speakerToggleButton != null) {
			speakerToggleButton.setEnabled(enabled);
		}
		if (micToggleButton != null) {
			micToggleButton.setEnabled(enabled);
		}
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

		if (recordingBorderOverlay != null) {
			recordingBorderOverlay.setVisible(false);
		}

		restoreLocationIfMoved();

		if (selectionFrame != null) {
			selectionFrame.dispose();
		}
	}

	public void cancelRecording() {
		if (recordingTimer != null) {
			recordingTimer.stop();
			recordingTimer = null;
		}

		elapsedSeconds = 0;

		if (recordingBorderOverlay != null) {
			recordingBorderOverlay.setVisible(false);
		}

		if (recorder == null) {
			restoreLocationIfMoved();
			setState(RecordingState.IDLE);
			return;
		}

		ScreenRecorder currentRecorder = recorder;
		File targetFile = currentRecorder.getOutputFile();

		recorder = null;

		setState(RecordingState.IDLE);

		Thread.ofVirtual().start(() -> {
			try {
				currentRecorder.cancel();
				deleteFile(targetFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		restoreLocationIfMoved();
	}

	private void deleteFile(File file) {
		if (file == null || !file.exists()) {
			return;
		}

		if (file.delete()) {
			System.out.println("Deleted recording: " + file.getAbsolutePath());
			return;
		}

		for (int attempt = 0; attempt < 10; attempt++) {
			try {

				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}

			if (!file.exists() || file.delete()) {
				System.out.println("Deleted recording: " + file.getAbsolutePath());
				return;
			}
		}

		file.deleteOnExit();
	}

	private void restoreLocationIfMoved() {
		if (preRecordingLocation != null) {
			setLocation(preRecordingLocation.x, preRecordingLocation.y);
			preRecordingLocation = null;
		}
	}

	@SuppressWarnings("exports")
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