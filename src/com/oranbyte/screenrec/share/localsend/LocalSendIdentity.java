package com.oranbyte.screenrec.share.localsend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.UUID;

public final class LocalSendIdentity {

	private static final String ALIAS = "ScreenRecorder";

	private static final String DEVICE_MODEL = "Windows";

	private static final String DEVICE_TYPE = LocalSendProtocol.DEVICE_TYPE_DESKTOP;

	private static final Path IDENTITY_FILE = Path.of(System.getProperty("user.home"), ".oranbyte", "screenrecorder",
			"localsend.properties");

	private static String fingerprint;

	private LocalSendIdentity() {
	}

	public static synchronized String getFingerprint() {

		if (fingerprint != null) {
			return fingerprint;
		}

		fingerprint = loadFingerprint();

		if (fingerprint == null || fingerprint.isBlank()) {

			fingerprint = UUID.randomUUID().toString();

			saveFingerprint(fingerprint);
		}

		return fingerprint;
	}

	private static String loadFingerprint() {

		if (!Files.exists(IDENTITY_FILE)) {
			return null;
		}

		Properties properties = new Properties();

		try (InputStream input = Files.newInputStream(IDENTITY_FILE)) {

			properties.load(input);

			return properties.getProperty("fingerprint");

		} catch (IOException e) {

			return null;
		}
	}

	private static void saveFingerprint(String value) {

		try {

			Files.createDirectories(IDENTITY_FILE.getParent());

			Properties properties = new Properties();

			properties.setProperty("fingerprint", value);

			try (OutputStream output = Files.newOutputStream(IDENTITY_FILE, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING)) {

				properties.store(output, "ScreenRecorder LocalSend identity");
			}

		} catch (IOException e) {

			throw new IllegalStateException("Unable to save LocalSend identity.", e);
		}
	}

	public static String getAlias() {
		return ALIAS;
	}

	public static String getDeviceModel() {
		return DEVICE_MODEL;
	}

	public static String getDeviceType() {
		return DEVICE_TYPE;
	}

	public static String getVersion() {
		return LocalSendProtocol.VERSION;
	}
}