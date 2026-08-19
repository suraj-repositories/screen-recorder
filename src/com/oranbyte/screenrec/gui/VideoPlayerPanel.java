package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.VideoProgressSlider;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class VideoPlayerPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JFXPanel fxPanel = new JFXPanel();
	private static final int DEFAULT_ICON_SIZE = 24;

	private static final Icons PLAY_ICON = Icons.PLAY_WHITE;
	private static final Icons PAUSE_ICON = Icons.PAUSE_WHITE;
	private static final Icons SPEAKER_ICON = Icons.SPEAKER;

	private static final int CONTROL_HEIGHT = 56;
	private static final int SIDE_MARGIN = 24;
	private static final int BOTTOM_MARGIN = 24;
	private static final int HIDE_DELAY_MS = 350;
	private static final int ANIM_TICK_MS = 15;
	private static final int ANIM_STEP = 18;

	private static final int CONTROLS_MAX_WIDTH = 600;

	private MediaPlayer mediaPlayer;

	private ToolbarButton playPauseButton;
	private ToolbarButton volumeButton;

	private VideoProgressSlider progressSlider;

	private JLabel elapsedTimeLabel;
	private JLabel remainingTimeLabel;

	private JLayeredPane layeredPane;
	private JPanel controlsPanel;

	private int controlsCurrentY;
	private int controlsTargetY;
	private boolean controlsVisible = true;
	private int lastPanelHeight = -1;
	private Timer hideTimer;
	private Timer animTimer;

	private File file;
	private MainFrame mainFrame;
	private Consumer<Dimension> onVideoReady;

	private static final int pad = 10;

	public VideoPlayerPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

		layeredPane = new JLayeredPane();
		layeredPane.setLayout(null);

		fxPanel.setBounds(0, 0, 0, 0);
		layeredPane.add(fxPanel, JLayeredPane.DEFAULT_LAYER);

		createControls();
		layeredPane.add(controlsPanel, JLayeredPane.PALETTE_LAYER);

		layeredPane.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutControlsPanel();
			}
		});

		add(layeredPane, BorderLayout.CENTER);

		setupHoverBehavior();
		setupAnimationTimer();
	}

	public VideoPlayerPanel(MainFrame mainFrame) {
		this();
		this.mainFrame = mainFrame;
	}

	private void createControls() {

		controlsPanel = new JPanel(new BorderLayout(12, 0)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(20, 20, 20, 190));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
				g2.dispose();
				super.paintComponent(g);
			}
		};

		controlsPanel.setOpaque(false);
		controlsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));

		playPauseButton = new ToolbarButton(PLAY_ICON, DEFAULT_ICON_SIZE);
		playPauseButton.setOpaque(false);
		playPauseButton.setContentAreaFilled(false);
		playPauseButton.setBorderPainted(false);
		playPauseButton.setFocusPainted(false);
		playPauseButton.setBorder(null);
		playPauseButton.addActionListener(e -> {

			if (mediaPlayer == null)
				return;

			Platform.runLater(() -> {

				MediaPlayer.Status status = mediaPlayer.getStatus();
				if (status == MediaPlayer.Status.PLAYING) {
					mediaPlayer.pause();
					SwingUtilities.invokeLater(() -> playPauseButton.setIconSize(PLAY_ICON, DEFAULT_ICON_SIZE));
				} else {
					mediaPlayer.play();
					SwingUtilities.invokeLater(() -> playPauseButton.setIconSize(PAUSE_ICON, DEFAULT_ICON_SIZE));
				}
			});
		});

		volumeButton = new ToolbarButton(SPEAKER_ICON, DEFAULT_ICON_SIZE);
		volumeButton.setOpaque(false);
		volumeButton.setContentAreaFilled(false);
		volumeButton.setBorderPainted(false);
		volumeButton.setFocusPainted(false);
		volumeButton.setBorder(null);
		volumeButton.addActionListener(e -> {

			if (mediaPlayer == null)
				return;
			Platform.runLater(() -> {
				boolean newMuteState = !mediaPlayer.isMute();
				mediaPlayer.setMute(newMuteState);
				volumeButton.setAllowed(!newMuteState);
			});
		});

		elapsedTimeLabel = new JLabel("00:00:00");
		elapsedTimeLabel.setForeground(Color.WHITE);
		elapsedTimeLabel.setFont(AppConstant.APP_FONT.deriveFont(13f));
		elapsedTimeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 8));

		remainingTimeLabel = new JLabel("00:00:00");
		remainingTimeLabel.setForeground(Color.WHITE);
		remainingTimeLabel.setFont(AppConstant.APP_FONT.deriveFont(13f));
		remainingTimeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 4));

		progressSlider = new VideoProgressSlider();
		progressSlider.addChangeListener(e -> {

			if (mediaPlayer == null)
				return;

			if (!progressSlider.getValueIsAdjusting())
				return;

			Duration total = mediaPlayer.getTotalDuration();
			if (total == null || total.isUnknown() || total.isIndefinite())
				return;

			double percent = progressSlider.getValue() / 100.0;
			double targetSeconds = total.toSeconds() * percent;

			Platform.runLater(() -> mediaPlayer.seek(Duration.seconds(targetSeconds)));
		});

		JPanel timelinePanel = new JPanel(new BorderLayout(8, 0));
		timelinePanel.setOpaque(false);
		timelinePanel.add(elapsedTimeLabel, BorderLayout.WEST);
		timelinePanel.add(progressSlider, BorderLayout.CENTER);
		timelinePanel.add(remainingTimeLabel, BorderLayout.EAST);

		controlsPanel.add(playPauseButton, BorderLayout.WEST);
		controlsPanel.add(timelinePanel, BorderLayout.CENTER);
		controlsPanel.add(volumeButton, BorderLayout.EAST);

	}

	private void layoutControlsPanel() {

		int w = layeredPane.getWidth();
		int h = layeredPane.getHeight();
		if (w <= 0 || h <= 0)
			return;

		fxPanel.setBounds(0, 0, w, h);

		if (h != lastPanelHeight) {
			controlsCurrentY = controlsVisible ? getVisibleY(h) : getHiddenY(h);
			lastPanelHeight = h;
		}

		int controlsWidth = getControlsWidth(w);
		int controlsX = getControlsX(w, controlsWidth);

		controlsPanel.setBounds(controlsX, controlsCurrentY, controlsWidth, CONTROL_HEIGHT);
		controlsPanel.revalidate();
		controlsPanel.repaint();

		setControlsTargetY(controlsVisible ? getVisibleY(h) : getHiddenY(h));
	}

	private int getControlsWidth(int parentWidth) {
		int available = Math.max(0, parentWidth - 2 * SIDE_MARGIN);
		return Math.min(CONTROLS_MAX_WIDTH, available);
	}

	private int getControlsX(int parentWidth, int controlsWidth) {
		return (parentWidth - controlsWidth) / 2;
	}

	private int getVisibleY(int panelHeight) {
		return panelHeight - CONTROL_HEIGHT - BOTTOM_MARGIN;
	}

	private int getHiddenY(int panelHeight) {
		return panelHeight + CONTROL_HEIGHT;
	}

	private void setupAnimationTimer() {

		controlsCurrentY = getHiddenY(0);

		animTimer = new Timer(ANIM_TICK_MS, e -> {

			int w = layeredPane.getWidth();
			int h = layeredPane.getHeight();
			int target = controlsVisible ? getVisibleY(h) : getHiddenY(h);

			if (controlsCurrentY == target) {
				((Timer) e.getSource()).stop();
				return;
			}

			int diff = target - controlsCurrentY;
			int step = Math.min(Math.abs(diff), ANIM_STEP);
			controlsCurrentY += (diff > 0) ? step : -step;

			int controlsWidth = getControlsWidth(w);
			int controlsX = getControlsX(w, controlsWidth);

			controlsPanel.setBounds(controlsX, controlsCurrentY, controlsWidth, CONTROL_HEIGHT);
			controlsPanel.revalidate();
			controlsPanel.repaint();
		});
	}

	private void showControls() {
		if (hideTimer != null) {
			hideTimer.stop();
		}
		if (!controlsVisible) {
			controlsVisible = true;
			animTimer.start();
		}
	}

	private void scheduleHideControls() {

		if (hideTimer != null) {
			hideTimer.stop();
		}

		hideTimer = new Timer(HIDE_DELAY_MS, e -> {
			controlsVisible = false;
			animTimer.start();
		});
		hideTimer.setRepeats(false);
		hideTimer.start();
	}

	private void setupHoverBehavior() {

		MouseAdapter hoverAdapter = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				showControls();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				scheduleHideControls();
			}
		};

		fxPanel.addMouseListener(hoverAdapter);
		addHoverRecursively(controlsPanel, hoverAdapter);
	}

	private void addHoverRecursively(java.awt.Component component, MouseAdapter hoverAdapter) {
		component.addMouseListener(hoverAdapter);
		if (component instanceof java.awt.Container) {
			for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
				addHoverRecursively(child, hoverAdapter);
			}
		}
	}

	public void play() {
		Platform.runLater(() -> {
			if (mediaPlayer != null) {
				mediaPlayer.play();
				SwingUtilities.invokeLater(() -> playPauseButton.setIconSize(PAUSE_ICON, DEFAULT_ICON_SIZE));
			}
		});
	}

	private void loadMedia() {

		Platform.runLater(() -> {

			if (mediaPlayer != null) {
				mediaPlayer.stop();
				mediaPlayer.dispose();
				mediaPlayer = null;
			}

			try {
				SwingUtilities.invokeLater(() -> progressSlider.setTotalSeconds(0));

				Media media = new Media(file.toURI().toASCIIString());
				mediaPlayer = new MediaPlayer(media);

				MediaView mediaView = new MediaView(mediaPlayer);
				mediaView.setPreserveRatio(true);

				mediaPlayer.setOnEndOfMedia(() -> {
					mediaPlayer.seek(Duration.ZERO);
					mediaPlayer.pause();

					SwingUtilities.invokeLater(() -> {
						playPauseButton.setIconSize(PLAY_ICON, DEFAULT_ICON_SIZE);
						progressSlider.setValue(0);
						elapsedTimeLabel.setText("00:00:00");

						Duration total = mediaPlayer.getTotalDuration();
						if (!total.isUnknown()) {
							remainingTimeLabel.setText(formatTime(total));
						}
					});
				});

				StackPane root = new StackPane(mediaView);

				fxPanel.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentResized(ComponentEvent e) {

						double width = fxPanel.getWidth();
						double height = fxPanel.getHeight();

						Platform.runLater(() -> {
							mediaView.setFitWidth(width);
							mediaView.setFitHeight(height);
						});
					}
				});

				fxPanel.setScene(new Scene(root));

				mediaPlayer.setOnReady(() -> {

					Dimension size = new Dimension((int) media.getWidth(), (int) media.getHeight());
					Duration total = mediaPlayer.getTotalDuration();

					SwingUtilities.invokeLater(() -> {
						if (onVideoReady != null) {
							onVideoReady.accept(size);
						}

						playPauseButton.setIconSize(PLAY_ICON, DEFAULT_ICON_SIZE);

						if (total != null && !total.isUnknown()) {
							remainingTimeLabel.setText(formatTime(total));
							progressSlider.setTotalSeconds(total.toSeconds());
						}

						updateProgress();
					});
				});

				mediaPlayer.setOnError(() -> System.out.println(mediaPlayer.getError()));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	private void updateProgress() {
		mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
			Duration total = mediaPlayer.getTotalDuration();

			if (total == null || total.isUnknown() || total.isIndefinite() || total.toSeconds() <= 0) {
				return;
			}

			double progress = (newTime.toSeconds() / total.toSeconds()) * 100;
			Duration remaining = total.subtract(newTime);

			SwingUtilities.invokeLater(() -> {
				if (!progressSlider.getValueIsAdjusting()) {
					progressSlider.setValue((int) progress);
				}

				elapsedTimeLabel.setText(formatTime(newTime));
				remainingTimeLabel.setText(formatTime(remaining));
			});
		});
	}

	private String formatTime(Duration duration) {
		long totalSeconds = (long) duration.toSeconds();

		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	public int getControlsTargetY() {
		return controlsTargetY;
	}

	public void setControlsTargetY(int controlsTargetY) {
		this.controlsTargetY = controlsTargetY;
	}

	public void open(String src) {
		this.file = new File(src);
		if (!file.exists() || file.length() == 0) {
			System.err.println("File does not exist or is empty: " + src);
			return;
		}
		loadMedia();
	}

	public void setOnVideoReady(Consumer<Dimension> onVideoReady) {
		this.onVideoReady = onVideoReady;
	}

}