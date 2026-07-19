package com.oranbyte.screenrec.constants;

public enum CaptureMode {
	RECTANGLE("Rectangle"), WINDOW("Window"), ENTIRE_SCREEN("Entire Screen");

	private final String displayName;

	CaptureMode(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}