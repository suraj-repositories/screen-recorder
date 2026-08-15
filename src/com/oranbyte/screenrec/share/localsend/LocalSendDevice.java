package com.oranbyte.screenrec.share.localsend;

import org.json.JSONObject;

import com.oranbyte.screenrec.share.ShareDevice;

public class LocalSendDevice extends ShareDevice {

	private final String fingerprint;

	private final String protocol;

	private final boolean downloadEnabled;

	private final String version;

	public LocalSendDevice(String fingerprint, String name, String address, int port, String deviceType,
			String deviceModel, String protocol, boolean downloadEnabled, String version) {

		super(fingerprint, name, address, port, deviceType, deviceModel);

		this.fingerprint = fingerprint;

		this.protocol = protocol;

		this.downloadEnabled = downloadEnabled;

		this.version = version;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public String getProtocol() {
		return protocol;
	}

	public boolean isDownloadEnabled() {
		return downloadEnabled;
	}

	public String getVersion() {
		return version;
	}

	public String getBaseUrl() {

		return protocol + "://" + getAddress() + ":" + getPort();
	}

	public static LocalSendDevice fromJson(String address, JSONObject json) {

		String fingerprint = json.optString("fingerprint", "");

		String alias = json.optString("alias", "Unknown Device");

		String deviceType = json.optString("deviceType", LocalSendProtocol.DEVICE_TYPE_DESKTOP);

		String deviceModel = json.optString("deviceModel", "");

		String protocol = json.optString("protocol", LocalSendProtocol.PROTOCOL_HTTPS);

		int port = json.optInt("port", LocalSendProtocol.DEFAULT_PORT);

		boolean download = json.optBoolean("download", false);

		String version = json.optString("version", LocalSendProtocol.VERSION);

		return new LocalSendDevice(fingerprint, alias, address, port, deviceType, deviceModel, protocol, download,
				version);
	}

	@Override
	public String toString() {

		return getName() + " [" + getDeviceModel() + "] " + getAddress() + ":" + getPort();
	}
}