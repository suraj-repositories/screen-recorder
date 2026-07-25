package com.oranbyte.screenrec.util;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;

public class NotificationUtil {

	private static TrayIcon trayIcon;

	static {
		if (SystemTray.isSupported()) {
			try {
				SystemTray tray = SystemTray.getSystemTray();

				Image image = Toolkit.getDefaultToolkit()
						.createImage(NotificationUtil.class.getResource("/com/oranbyte/screenrec/icons/favicon.png"));

				trayIcon = new TrayIcon(image, "Screen Recorder");
				trayIcon.setImageAutoSize(true);

				tray.add(trayIcon);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void notify(String title, String message) {
		System.out.println(trayIcon);
		if (trayIcon != null) {
			trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
		}
	}
}