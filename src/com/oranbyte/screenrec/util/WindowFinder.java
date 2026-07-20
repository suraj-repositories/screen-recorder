package com.oranbyte.screenrec.util;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.win32.W32APIOptions;

public class WindowFinder {

	private interface User32Ext extends User32 {
		User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean IsIconic(HWND hWnd);
	}

	private interface Dwmapi extends Library {
		Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class, W32APIOptions.DEFAULT_OPTIONS);

		int DWMWA_EXTENDED_FRAME_BOUNDS = 9;

		int DwmGetWindowAttribute(HWND hwnd, int dwAttribute, RECT pvAttribute, int cbAttribute);
	}

	public static Rectangle findWindowAt(Point screenPoint, Component excluded) {
		HWND excludedHwnd = excluded != null ? new HWND(Native.getComponentPointer(excluded)) : null;

		List<HWND> windows = new ArrayList<>();
		User32Ext.INSTANCE.EnumWindows((hWnd, data) -> {
			windows.add(hWnd);
			return true;
		}, Pointer.NULL);

		for (HWND hwnd : windows) {
			if (hwnd.equals(excludedHwnd)) {
				continue;
			}
			if (!User32Ext.INSTANCE.IsWindowVisible(hwnd)) {
				continue;
			}
			if (User32Ext.INSTANCE.IsIconic(hwnd)) {
				continue;
			}
			if (User32Ext.INSTANCE.GetWindowTextLength(hwnd) == 0) {
				continue;
			}

			Rectangle bounds = getVisibleBounds(hwnd);
			if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
				continue;
			}
			if (bounds.contains(screenPoint)) {
				return bounds;
			}
		}

		return null;
	}

	private static Rectangle getVisibleBounds(HWND hwnd) {
		RECT rect = new RECT();
		int hr = Dwmapi.INSTANCE.DwmGetWindowAttribute(hwnd, Dwmapi.DWMWA_EXTENDED_FRAME_BOUNDS, rect, rect.size());

		if (hr != 0) {
			if (!User32Ext.INSTANCE.GetWindowRect(hwnd, rect)) {
				return null;
			}
		}

		return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
	}
}