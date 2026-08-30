package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicPopupMenuUI;

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

	private static final Icons PLAY_ICON = Icons.PLAY_WHITE;
	private static final Icons PAUSE_ICON = Icons.PAUSE_WHITE;
	private static final Icons SPEAKER_ICON = Icons.SPEAKER;

	private static final int DEFAULT_ICON_SIZE = 24;
	private static final int CONTROL_HEIGHT = 56;
	private static final int SIDE_MARGIN = 24;
	private static final int BOTTOM_MARGIN = 24;
	private static final int HIDE_DELAY_MS = 350;
	private static final int ANIM_TICK_MS = 15;
	private static final int ANIM_STEP = 18;
	private static final int CONTROLS_MAX_WIDTH = 600;
	private static final int MAX_MEDIA_RETRIES = 3;
	private static final long MEDIA_RETRY_DELAY_MS = 1000;

	private static final boolean ENABLE_LOGGING = false;

	private volatile MediaPlayer mediaPlayer;

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
	private Consumer<Dimension> onVideoReady;
	private JPopupMenu volumePopup;
	private long lastPopupCloseTime = 0;

	private static final int pad = 10;
	private MediaView currentMediaView;

	public VideoPlayerPanel() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

		layeredPane = new JLayeredPane();
		layeredPane.setLayout(null);

		fxPanel.setBounds(0, 0, 0, 0);
		fxPanel.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {

				MediaView view = currentMediaView;

				if (view == null) {
					return;
				}

				double width = fxPanel.getWidth();
				double height = fxPanel.getHeight();

				Platform.runLater(() -> {
					view.setFitWidth(width);
					view.setFitHeight(height);
				});
			}
		});
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
		playPauseButton.setTransprent();
		playPauseButton.setContentAreaFilled(false);
		playPauseButton.setBorderPainted(false);
		playPauseButton.setFocusPainted(false);
		playPauseButton.setBorder(null);
		playPauseButton.addActionListener(e -> {

			if (mediaPlayer == null)
				return;

			Platform.runLater(() -> {

				MediaPlayer player = mediaPlayer;
				if (player == null) {
					return;
				}

				MediaPlayer.Status status = player.getStatus();
				if (status == MediaPlayer.Status.PLAYING) {
					player.pause();
					SwingUtilities.invokeLater(() -> playPauseButton.setIconSize(PLAY_ICON, DEFAULT_ICON_SIZE));
				} else {
					player.play();
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
		volumeButton.setTransprent();

		volumeButton.addActionListener(e -> {
			if (mediaPlayer == null) {
				return;
			}

			if (System.currentTimeMillis() - lastPopupCloseTime < 250) {
				return;
			}

			if (isVolumePopupShowing()) {
				closeVolumePopup();
			} else {
				showVolumePopup();
			}
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

			MediaPlayer player = mediaPlayer;
			if (player == null)
				return;

			if (!progressSlider.getValueIsAdjusting())
				return;

			Duration total = player.getTotalDuration();
			if (total == null || total.isUnknown() || total.isIndefinite())
				return;

			double percent = progressSlider.getValue() / 100.0;
			double targetSeconds = total.toSeconds() * percent;

			Platform.runLater(() -> {
				if (mediaPlayer == player) {
					player.seek(Duration.seconds(targetSeconds));
				}
			});
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

	private boolean isVolumePopupShowing() {
		return volumePopup != null && volumePopup.isVisible();
	}

	private boolean isMouseOverPanel() {
		if (layeredPane == null || !layeredPane.isShowing()) {
			return false;
		}
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(mousePos, layeredPane);
		return layeredPane.contains(mousePos);
	}

	private void showVolumePopup() {
		MediaPlayer player = mediaPlayer;
		if (player == null)
			return;

		if (volumePopup != null) {
			volumePopup.setVisible(false);
			volumePopup = null;
		}

		JPanel panel = new JPanel();

		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

		boolean isMuted = player.isMute();
		ToolbarButton muteButton = new ToolbarButton(Icons.SPEAKER, DEFAULT_ICON_SIZE);
		muteButton.setAllowed(!isMuted);
		muteButton.setOpaque(false);
		muteButton.setContentAreaFilled(false);
		muteButton.setBorderPainted(false);
		muteButton.setTransprent();
		muteButton.setFocusPainted(false);
		muteButton.setBorder(null);
		muteButton.addActionListener(e -> {
			Platform.runLater(() -> {
				MediaPlayer p = mediaPlayer;
				if (p == null) {
					return;
				}
				boolean newMuteState = !p.isMute();
				p.setMute(newMuteState);

				SwingUtilities.invokeLater(() -> {
					muteButton.setIconSize(Icons.SPEAKER, DEFAULT_ICON_SIZE);
					muteButton.setAllowed(!newMuteState);
					volumeButton.setAllowed(!newMuteState);
				});
			});
		});

		VideoProgressSlider volumeSlider = new VideoProgressSlider();
		volumeSlider.setMinimum(0);
		volumeSlider.setMaximum(100);
		volumeSlider.setValue((int) Math.round(player.getVolume() * 100));
		volumeSlider.setPreferredSize(new Dimension(150, 20));
		volumeSlider.setMaximumSize(new Dimension(150, 20));

		JLabel volumeLabel = new JLabel(volumeSlider.getValue() + "%");
		volumeLabel.setForeground(Color.WHITE);
		volumeLabel.setFont(AppConstant.APP_FONT.deriveFont(12f));

		volumeSlider.addChangeListener(e -> {
			int value = volumeSlider.getValue();
			volumeLabel.setText(value + "%");
			Platform.runLater(() -> {
				MediaPlayer p = mediaPlayer;
				if (p != null) {
					p.setVolume(value / 100.0);
				}
			});
		});

		panel.add(muteButton);
		panel.add(volumeSlider);
		panel.add(Box.createHorizontalStrut(8));
		panel.add(volumeLabel);

		MouseAdapter popupHoverAdapter = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				showControls();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				scheduleHideControls();
			}
		};
		addHoverRecursively(panel, popupHoverAdapter);

		volumePopup = new JPopupMenu() {
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				setUI(new BasicPopupMenuUI());
				setBorder(BorderFactory.createEmptyBorder());
				setOpaque(false);
			}

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(30, 30, 30, 220));
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		volumePopup.setFocusable(false);
		volumePopup.setBorder(BorderFactory.createEmptyBorder());
		volumePopup.setOpaque(false);
		volumePopup.setBackground(new Color(0, 0, 0, 0));
		volumePopup.setLayout(new BorderLayout());
		volumePopup.add(panel, BorderLayout.CENTER);

		volumePopup.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(() -> {
					Window window = SwingUtilities.getWindowAncestor(volumePopup);
					if (window instanceof Frame frame) {
						if (frame.isUndecorated()) {
							frame.setBackground(new Color(0, 0, 0, 0));
						}
					} else if (window != null) {
						window.setBackground(new Color(0, 0, 0, 0));
					}
				});
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				lastPopupCloseTime = System.currentTimeMillis();
				SwingUtilities.invokeLater(() -> {
					if (volumePopup != null && !volumePopup.isVisible()) {
						volumePopup = null;
					}
					scheduleHideControls();
				});
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				lastPopupCloseTime = System.currentTimeMillis();
				SwingUtilities.invokeLater(() -> {
					volumePopup = null;
					scheduleHideControls();
				});
			}
		});

		Dimension popupSize = panel.getPreferredSize();
		int x = (volumeButton.getWidth() - popupSize.width) / 2;
		int y = -popupSize.height - 12;

		volumePopup.show(volumeButton, x, y);
	}

	private void closeVolumePopup() {
		if (volumePopup != null) {
			lastPopupCloseTime = System.currentTimeMillis();
			JPopupMenu popup = volumePopup;
			volumePopup = null;

			popup.setVisible(false);
			popup.removeAll();
		}

		scheduleHideControls();
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

		if (isVolumePopupShowing()) {
			return;
		}

		if (isMouseOverPanel()) {
			showControls();
			return;
		}

		if (hideTimer != null) {
			hideTimer.stop();
		}

		hideTimer = new Timer(HIDE_DELAY_MS, e -> {
			if (!isVolumePopupShowing() && !isMouseOverPanel()) {
				controlsVisible = false;
				animTimer.start();
			}
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
			MediaPlayer player = mediaPlayer;
			if (player != null) {
				player.play();
				SwingUtilities.invokeLater(() -> playPauseButton.setIconSize(PAUSE_ICON, DEFAULT_ICON_SIZE));
			}
		});
	}

	private void loadMedia() {
		loadMedia(0);
	}

	private void loadMedia(int retryCount) {

		Platform.runLater(() -> {

			MediaPlayer oldPlayer = mediaPlayer;

			if (oldPlayer != null) {
				mediaPlayer = null;
				currentMediaView = null;

				try {
					oldPlayer.stop();
				} catch (Exception ignored) {
				}

				try {
					oldPlayer.dispose();
				} catch (Exception ignored) {
				}
			}

			try {

				SwingUtilities.invokeLater(() -> progressSlider.setTotalSeconds(0));

				if (file == null || !file.isFile()) {
					if (ENABLE_LOGGING) {
						System.err.println("Invalid media file: " + file);
					}
					return;
				}
				waitForFileStableAsync(file, stable -> {

					if (!stable) {
						if (ENABLE_LOGGING) {
							System.err.println("File did not become stable: " + file.getAbsolutePath());
						}

						retryLoad(retryCount);
						return;
					}

					Platform.runLater(() -> createMediaPlayer(retryCount));
				});

			} catch (Exception ex) {
				if (ENABLE_LOGGING) {
					System.err.println("Failed while preparing media");
					ex.printStackTrace();
				}

				retryLoad(retryCount);
			}
		});
	}

	private void createMediaPlayer(int retryCount) {

		try {

			if (file == null || !file.isFile()) {
				if (ENABLE_LOGGING) {
					System.err.println("Invalid media file: " + file);
				}
				return;
			}

			String uri = file.toURI().toString();

			if (ENABLE_LOGGING) {
				System.out.println("================================");
				System.out.println("Loading media");
				System.out.println("Attempt  : " + (retryCount + 1));
				System.out.println("File     : " + file.getAbsolutePath());
				System.out.println("Exists   : " + file.exists());
				System.out.println("Size     : " + file.length());
				System.out.println("URI      : " + uri);
				System.out.println("================================");
			}

			Media media = new Media(uri);

			MediaPlayer player = new MediaPlayer(media);

			mediaPlayer = player;

			MediaView mediaView = new MediaView(player);
			mediaView.setPreserveRatio(true);
			mediaView.setFitWidth(fxPanel.getWidth());
			mediaView.setFitHeight(fxPanel.getHeight());
			currentMediaView = mediaView;

			StackPane root = new StackPane(mediaView);

			fxPanel.setScene(new Scene(root));

			player.setOnReady(() -> {

				if (ENABLE_LOGGING) {
					System.out.println("Media READY: " + file.getName());
				}

				int width = (int) media.getWidth();
				int height = (int) media.getHeight();

				Dimension size = new Dimension(width, height);
				Duration total = player.getTotalDuration();

				SwingUtilities.invokeLater(() -> {
					if (onVideoReady != null) {
						onVideoReady.accept(size);
					}
					playPauseButton.setIconSize(PLAY_ICON, DEFAULT_ICON_SIZE);

					if (total != null && !total.isUnknown()) {
						remainingTimeLabel.setText(formatTime(total));
						progressSlider.setTotalSeconds(total.toSeconds());
					}
				});
			});

			player.setOnEndOfMedia(() -> {
				if (mediaPlayer != player) {
					return;
				}

				player.seek(Duration.ZERO);
				player.pause();

				SwingUtilities.invokeLater(() -> {
					playPauseButton.setIconSize(PLAY_ICON, DEFAULT_ICON_SIZE);
					progressSlider.setValue(0);
					elapsedTimeLabel.setText("00:00:00");
					Duration total = player.getTotalDuration();

					if (total != null && !total.isUnknown()) {
						remainingTimeLabel.setText(formatTime(total));
					}
				});
			});

			player.setOnError(() -> {

				Throwable error = player.getError();

				if (ENABLE_LOGGING) {
					System.err.println("================================");
					System.err.println("JavaFX MediaPlayer ERROR");
					System.err.println("Attempt : " + (retryCount + 1));
					System.err.println("File    : " + file.getAbsolutePath());
					System.err.println("Size    : " + file.length());
					System.err.println("URI     : " + uri);
					if (error != null) {
						error.printStackTrace();
					}

					System.err.println("================================");
				}

				if (mediaPlayer == player) {

					mediaPlayer = null;
					currentMediaView = null;

					try {
						player.stop();
					} catch (Exception ignored) {
					}

					try {
						player.dispose();
					} catch (Exception ignored) {
					}

					retryLoad(retryCount);
				}
			});
 
			updateProgress(player);

		} catch (Exception ex) {
			if (ENABLE_LOGGING) {
				System.err.println("Failed to create JavaFX media player");
				System.err.println("Attempt: " + (retryCount + 1));
				System.err.println("File: " + file);
				ex.printStackTrace();
			}

			retryLoad(retryCount);
		}
	}

	private void retryLoad(int retryCount) {

		if (retryCount >= MAX_MEDIA_RETRIES) {

			if (ENABLE_LOGGING) {
				System.err.println("Media loading failed after " + MAX_MEDIA_RETRIES + " retries: " + file);
			}

			return;
		}

		int nextAttempt = retryCount + 1;
		if (ENABLE_LOGGING) {
			System.out.println("Retrying media load in " + MEDIA_RETRY_DELAY_MS + " ms...");
		}

		Thread retryThread = new Thread(() -> {

			try {
				Thread.sleep(MEDIA_RETRY_DELAY_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}

			loadMedia(nextAttempt);

		}, "Media-Retry-" + nextAttempt);

		retryThread.setDaemon(true);
		retryThread.start();
	}

	private void waitForFileStableAsync(File file, java.util.function.Consumer<Boolean> callback) {

		Thread thread = new Thread(() -> {
			long previousSize = -1;

			for (int i = 0; i < 20; i++) {

				long currentSize = file.length();
				if (currentSize > 0 && currentSize == previousSize) {
					callback.accept(true);
					return;
				}
				previousSize = currentSize;
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					callback.accept(false);
					return;
				}
			}
			callback.accept(false);
		}, "Media-File-Stability-Check");

		thread.setDaemon(true);
		thread.start();
	}

	 
	private void updateProgress(MediaPlayer player) {
		player.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
 
			if (mediaPlayer != player) {
				return;
			}

			Duration total = player.getTotalDuration();

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
			if (ENABLE_LOGGING) {
				System.err.println("File does not exist or is empty: " + src);
			}
			return;
		}
		loadMedia();
	}

	public void setOnVideoReady(@SuppressWarnings("exports") Consumer<Dimension> onVideoReady) {
		this.onVideoReady = onVideoReady;
	}

}