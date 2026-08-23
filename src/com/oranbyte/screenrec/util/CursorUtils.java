package com.oranbyte.screenrec.util;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HICON;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinGDI.ICONINFO;
import com.sun.jna.win32.W32APIOptions;

public final class CursorUtils {

	private CursorUtils() {
	}

	@Structure.FieldOrder({ "cbSize", "flags", "hCursor", "ptScreenPos" })
	public static class CURSORINFO extends Structure {
		public int cbSize;
		public int flags;
		public HICON hCursor;
		public POINT ptScreenPos;

		public CURSORINFO() {
			super();
			cbSize = size();
		}

		public CURSORINFO(com.sun.jna.Pointer p) {
			super(p);
			read();
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("cbSize", "flags", "hCursor", "ptScreenPos");
		}
	}

	public interface MyUser32 extends User32 {
		MyUser32 INSTANCE = Native.load("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean GetCursorInfo(CURSORINFO pci);
	}

	public static class CursorSnapshot {
		public BufferedImage image;
		public int hotspotX;
		public int hotspotY;
		public int screenX;
		public int screenY;
	}

	public static final int CURSOR_SHOWING = 0x00000001;

	public static CursorSnapshot capture() {
		try {
			CURSORINFO cursorInfo = new CURSORINFO();

			if (!MyUser32.INSTANCE.GetCursorInfo(cursorInfo)) {
				System.out.println("GetCursorInfo failed, cbSize=" + cursorInfo.cbSize);
				return null;
			}
			if ((cursorInfo.flags & CURSOR_SHOWING) == 0) {
				System.out.println("Cursor not showing, flags=" + cursorInfo.flags);
				return null;
			}
			HICON hIcon = cursorInfo.hCursor;
			if (hIcon == null) {
				System.out.println("hCursor is null");
				return null;
			}
			ICONINFO iconInfo = new ICONINFO();
			try {
				if (!User32.INSTANCE.GetIconInfo(hIcon, iconInfo)) {
					System.out.println("GetIconInfo failed");
					return null;
				}
				BufferedImage image = Win32IconUtil.hIconToBufferedImage(hIcon);
				if (image == null) {
					System.out.println("hIconToBufferedImage returned null");
					return null;
				}

				CursorSnapshot snapshot = new CursorSnapshot();
				snapshot.image = image;
				snapshot.hotspotX = iconInfo.xHotspot;
				snapshot.hotspotY = iconInfo.yHotspot;
				snapshot.screenX = cursorInfo.ptScreenPos.x;
				snapshot.screenY = cursorInfo.ptScreenPos.y;

				return snapshot;

			} finally {
				if (iconInfo.hbmColor != null) {
					GDI32.INSTANCE.DeleteObject(iconInfo.hbmColor);
				}
				if (iconInfo.hbmMask != null) {
					GDI32.INSTANCE.DeleteObject(iconInfo.hbmMask);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}
}