package com.oranbyte.screenrec.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import com.oranbyte.screenrec.constants.Icons;

public final class FileUtil {

	private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

	private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mkv", "avi", "mov", "webm", "wmv", "flv");

	private FileUtil() {
	}

	public static boolean isImage(File file) {
		return hasExtension(file, IMAGE_EXTENSIONS);
	}

	public static boolean isVideo(File file) {
		return hasExtension(file, VIDEO_EXTENSIONS);
	}

	private static boolean hasExtension(File file, Set<String> extensions) {
		if (file == null || !file.isFile()) {
			return false;
		}

		String name = file.getName();
		int index = name.lastIndexOf('.');

		if (index < 0 || index == name.length() - 1) {
			return false;
		}

		String extension = name.substring(index + 1).toLowerCase();

		return extensions.contains(extension);
	}

	@SuppressWarnings("exports")
	public static Icon resolveFileIcon(File file, int size) {
		if (file == null)
			return new ImageIcon();
		try {
			Icon systemIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
			if (systemIcon != null)
				return scaleIcon(systemIcon, size) ;
		} catch (Exception ignored) {
		}
		return scaleIcon(Icons.FILE.icon() , size);
	}

	private static Icon scaleIcon(Icon icon, int targetWidth) {
		if (icon == null)
			return null;
		int srcW = icon.getIconWidth();
		int srcH = icon.getIconHeight();
		double scale = (double) targetWidth / srcW;
		int targetHeight = (int) Math.round(srcH * scale);

		BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.scale(scale, scale);
		icon.paintIcon(null, g2, 0, 0);
		g2.dispose();
		return new ImageIcon(image);
	}
}