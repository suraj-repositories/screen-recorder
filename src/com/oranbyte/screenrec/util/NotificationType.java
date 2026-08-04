package com.oranbyte.screenrec.util;

import java.awt.TrayIcon;

public enum NotificationType {

	SUCCESS(TrayIcon.MessageType.INFO), INFO(TrayIcon.MessageType.INFO), WARNING(TrayIcon.MessageType.WARNING),
	ERROR(TrayIcon.MessageType.ERROR);

	private final TrayIcon.MessageType trayType;

	NotificationType(TrayIcon.MessageType trayType) {
		this.trayType = trayType;
	}

	public TrayIcon.MessageType getTrayType() {
		return trayType;
	}
}