package com.oranbyte.screenrec.constants;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.ImageIcon;

public enum Icons {

	PLUS("plus.png"), FAVICON("favicon.png"), CAMERA("camera.png"), VIDEO("video.png"), CLOSE("close.png"),
	MICROPHONE("microphone.png"), START("start.png"), STOP("stop.png"), PAUSE("pause.png"), PLAY("play.png"),
	VOLUME("volume.png"), PLAY_VIDEO_CIRCLE("play_video_circle.png"), SPEAKER("speaker.png"),
	PLAY_WHITE("play-white.png"), PAUSE_WHITE("pause-white.png"), SAVE("save.png"), COPY("copy.png"),
	SHARE("share.png"), CHECK_GREEN("check-green.png"), DRAG("drag.png"), WHATSAPP("whatsapp.png"),
	TELEGRAM("telegram.png"), TWITTER("twitter.png"), EMAIL("email.png"), RECTANGLE("rectangle.png"), WINDOW("window.png"),
	ENTIRE_SCREEN("full-screen.png"), DROPPER("dropper.png"), FOLDER("folder.png"), IMAGE("image.png");

	private static final String BASE_PATH = "/com/oranbyte/screenrec/icons/";

	private final ImageIcon icon;
	private final String path;
	private File cachedFile;

	Icons(String fileName) {
		this.path = BASE_PATH + fileName;

		URL url = Icons.class.getResource(path);
		if (url == null) {
			throw new IllegalStateException("Missing icon: " + path);
		}

		this.icon = new ImageIcon(url);
	}

	public ImageIcon icon() {
		return icon;
	}

	public ImageIcon icon(int size) {
		Image image = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
		return new ImageIcon(image);
	}

	public synchronized File file() {
		if (cachedFile != null && cachedFile.exists()) {
			return cachedFile;
		}

		try (InputStream in = Icons.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing icon: " + path);
			}

			String extension = path.substring(path.lastIndexOf('.'));
			cachedFile = File.createTempFile(name().toLowerCase() + "_", extension);
			cachedFile.deleteOnExit();

			Files.copy(in, cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return cachedFile;

		} catch (IOException e) {
			throw new RuntimeException("Failed to create temporary icon file: " + path, e);
		}
	}
}