package com.oranbyte.screenrec.share.localsend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import org.json.JSONObject;

public class LocalSendFile {

	private final String id;

	private final File file;

	public LocalSendFile(File file) {

		if (file == null) {
			throw new IllegalArgumentException("File cannot be null.");
		}

		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
		}

		if (!file.isFile()) {
			throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
		}

		this.id = UUID.randomUUID().toString();

		this.file = file;
	}

	public String getId() {
		return id;
	}

	public File getFile() {
		return file;
	}

	public String getFileName() {
		return file.getName();
	}

	public long getSize() {
		return file.length();
	}

	public String getMimeType() {

		try {

			String type = Files.probeContentType(file.toPath());

			if (type != null) {
				return type;
			}

		} catch (IOException ignored) {
		}

		return guessMimeType(file.getName());
	}

	public String getFileType() {
		return getMimeType();
	}

	private String guessMimeType(String name) {

		String lower = name.toLowerCase();

		if (lower.endsWith(".mp4")) {
			return "video/mp4";
		}

		if (lower.endsWith(".mov")) {
			return "video/quicktime";
		}

		if (lower.endsWith(".mkv")) {
			return "video/x-matroska";
		}

		if (lower.endsWith(".avi")) {
			return "video/x-msvideo";
		}

		if (lower.endsWith(".webm")) {
			return "video/webm";
		}

		if (lower.endsWith(".png")) {
			return "image/png";
		}

		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {

			return "image/jpeg";
		}

		if (lower.endsWith(".gif")) {
			return "image/gif";
		}

		if (lower.endsWith(".bmp")) {
			return "image/bmp";
		}

		if (lower.endsWith(".pdf")) {
			return "application/pdf";
		}

		if (lower.endsWith(".txt")) {
			return "text/plain";
		}

		if (lower.endsWith(".zip")) {
			return "application/zip";
		}

		return "application/octet-stream";
	}

	public String sha256() throws IOException {

		try {

			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			try (InputStream input = Files.newInputStream(file.toPath())) {

				byte[] buffer = new byte[1024 * 1024];

				int read;

				while ((read = input.read(buffer)) != -1) {

					digest.update(buffer, 0, read);
				}
			}

			return HexFormat.of().formatHex(digest.digest());

		} catch (Exception e) {

			throw new IOException("Unable to calculate SHA-256.", e);
		}
	}

	public JSONObject toJson(boolean includeHash) throws IOException {

		JSONObject json = new JSONObject();

		json.put("id", id);

		json.put("fileName", getFileName());

		json.put("size", getSize());

		json.put("fileType", getMimeType());

		if (includeHash) {

			json.put("sha256", sha256());
		}

		return json;
	}
}