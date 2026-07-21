package com.oranbyte.screenrec.constants;

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

public enum Icons {

	PLUS("plus.png"), FAVICON("favicon.png"), CAMERA("camera.png"), VIDEO("video.png"), CLOSE("close.png"),
	MICROPHONE("microphone.png"), START("start.png"), STOP("stop.png"), PAUSE("pause.png"), PLAY("play.png"),
	VOLUME("volume.png");

	private static final String BASE_PATH = "/com/oranbyte/screenrec/icons/";

	private final ImageIcon icon;

	Icons(String fileName) {
		URL url = Icons.class.getResource(BASE_PATH + fileName);

		if (url == null) {
			throw new IllegalStateException("Missing icon: " + BASE_PATH + fileName);
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
}