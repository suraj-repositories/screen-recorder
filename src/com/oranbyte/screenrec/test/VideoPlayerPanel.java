package com.oranbyte.screenrec.test;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.components.ToolbarButton;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class VideoPlayerPanel extends JPanel {

	private final JFXPanel fxPanel = new JFXPanel();
	private static final int DEFAULT_ICON_SIZE = 20;

	private MediaPlayer mediaPlayer;

	private ToolbarButton playPauseButton;
	private ToolbarButton volumeButton;

	private JSlider progressSlider;

	private JLabel timeLabel;

	public VideoPlayerPanel() {

		setLayout(new BorderLayout());

		add(fxPanel, BorderLayout.CENTER);

		createControls();
	}

	private void createControls() {

		JPanel controls = new JPanel(new BorderLayout());

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

		playPauseButton = new ToolbarButton(Icons.PLAY, DEFAULT_ICON_SIZE);

		playPauseButton.addActionListener(e -> {

			if (mediaPlayer == null)
				return;

			Platform.runLater(() -> {

				MediaPlayer.Status status = mediaPlayer.getStatus();

				if (status == MediaPlayer.Status.PLAYING) {

					mediaPlayer.pause();

					SwingUtilities.invokeLater(() -> playPauseButton.setIconSize(Icons.PAUSE, DEFAULT_ICON_SIZE));

				} else {

					mediaPlayer.play();

					SwingUtilities.invokeLater(() -> playPauseButton.setIconSize(Icons.PLAY, DEFAULT_ICON_SIZE));
				}
			});
		});

		volumeButton = new ToolbarButton(Icons.SPEAKER, DEFAULT_ICON_SIZE);

		volumeButton.addActionListener(e -> {

			if (mediaPlayer == null)
				return;

			Platform.runLater(() -> {

				boolean mute = mediaPlayer.isMute();

				mediaPlayer.setMute(!mute);

				volumeButton.setAllowed(!mute);

			});

		});

		timeLabel = new JLabel("00:00 / 00:00");
		timeLabel.setFont(AppConstant.APP_FONT.deriveFont(14f));

		buttons.add(playPauseButton);
		buttons.add(volumeButton);

		progressSlider = new VideoProgressSlider();

		progressSlider.addChangeListener(e -> {

			if (mediaPlayer == null)
				return;

			if (progressSlider.getValueIsAdjusting()) {

				Platform.runLater(() -> {

					Duration total = mediaPlayer.getTotalDuration();

					double seconds = total.toSeconds() * progressSlider.getValue() / 100.0;

					mediaPlayer.seek(Duration.seconds(seconds));

				});
			}
		});

		controls.add(buttons, BorderLayout.WEST);
		controls.add(progressSlider, BorderLayout.CENTER);
		controls.add(timeLabel, BorderLayout.EAST);

		add(controls, BorderLayout.SOUTH);
	}

	public void play(File file) {

		Platform.runLater(() -> {

			Media media = new Media(file.toURI().toString());

			mediaPlayer = new MediaPlayer(media);

			MediaView mediaView = new MediaView(mediaPlayer);

			mediaView.setPreserveRatio(true);

			StackPane root = new StackPane(mediaView);

			// Make video resize with parent
			fxPanel.addComponentListener(new java.awt.event.ComponentAdapter() {

				@Override
				public void componentResized(java.awt.event.ComponentEvent e) {

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

				mediaPlayer.play();

				updateProgress();

				System.out.println("Video Ready");

			});

			mediaPlayer.setOnError(() -> {

				System.out.println(mediaPlayer.getError());

			});

		});
	}

	private void updateProgress() {

		mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {

			Duration total = mediaPlayer.getTotalDuration();

			if (total.isUnknown())
				return;

			double progress = newTime.toSeconds() / total.toSeconds() * 100;

			SwingUtilities.invokeLater(() -> {

				progressSlider.setValue((int) progress);

				timeLabel.setText(formatTime(newTime) + " / " + formatTime(total));

			});

		});

	}

	private String formatTime(Duration duration) {

		int seconds = (int) duration.toSeconds();

		int min = seconds / 60;

		int sec = seconds % 60;

		return String.format("%02d:%02d", min, sec);
	}
}