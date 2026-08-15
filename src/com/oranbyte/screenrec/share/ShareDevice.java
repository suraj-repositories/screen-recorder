package com.oranbyte.screenrec.share;

import java.util.Objects;

public class ShareDevice {

	private final String id;
	private final String name;
	private final String address;
	private final int port;
	private final String deviceType;
	private final String deviceModel;

	public ShareDevice(String id, String name, String address, int port, String deviceType, String deviceModel) {

		this.id = Objects.requireNonNull(id, "id");
		this.name = Objects.requireNonNull(name, "name");
		this.address = Objects.requireNonNull(address, "address");
		this.port = port;
		this.deviceType = deviceType;
		this.deviceModel = deviceModel;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public String getDeviceModel() {
		return deviceModel;
	}

	public String getEndpoint() {
		return address + ":" + port;
	}

	@Override
	public String toString() {
		return name + " (" + address + ":" + port + ")";
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof ShareDevice other)) {
			return false;
		}

		return id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}