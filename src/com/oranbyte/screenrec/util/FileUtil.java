package com.oranbyte.screenrec.util;

import java.io.File;
import java.util.Set;

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
}