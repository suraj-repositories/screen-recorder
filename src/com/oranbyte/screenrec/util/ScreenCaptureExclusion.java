package com.oranbyte.screenrec.util;

import java.awt.Window;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public class ScreenCaptureExclusion {

	private static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;

	private interface User32Ext extends StdCallLibrary {
		User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean SetWindowDisplayAffinity(Pointer hwnd, int dwAffinity);
	}

	public static void excludeFromCapture(Window window) {
		if (!isWindows()) {
			return;
		}
		try {
			Pointer hwnd = Native.getComponentPointer(window);
			if (hwnd != null) {
				User32Ext.INSTANCE.SetWindowDisplayAffinity(hwnd, WDA_EXCLUDEFROMCAPTURE);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}
}