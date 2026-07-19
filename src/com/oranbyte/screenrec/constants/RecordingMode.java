package com.oranbyte.screenrec.constants;

public enum RecordingMode {

	SCREENSHOT(0, "Screenshot"), VIDEO(1, "Video");

	private final int value;
	private final String displayName;

	RecordingMode(int value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public int getValue() {
		return value;
	}

	public static RecordingMode fromValue(int value) {
		for (RecordingMode mode : values()) {
			if (mode.value == value) {
				return mode;
			}
		}
		return SCREENSHOT;
	}

	@Override
	public String toString() {
		return displayName;
	}
}