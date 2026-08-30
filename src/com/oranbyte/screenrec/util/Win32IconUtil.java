package com.oranbyte.screenrec.util;

import java.awt.image.BufferedImage;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HICON;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

public class Win32IconUtil {

	public static final int CURSOR_SIZE = 32;

	public interface MyUser32 extends User32 {
		MyUser32 INSTANCE = Native.load("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean DrawIcon(@SuppressWarnings("exports") HDC hDC, int X, int Y, @SuppressWarnings("exports") HICON hIcon);

		boolean DrawIconEx(@SuppressWarnings("exports") HDC hdc, int xLeft, int yTop, @SuppressWarnings("exports") HICON hIcon, int cxWidth, int cyWidth, int istepIfAniCur,
				@SuppressWarnings("exports") Pointer hbrFlickerFreeDraw, int diFlags);
	}

	@SuppressWarnings("exports")
	public static BufferedImage hIconToBufferedImage(HICON hIcon) {
		int size = 32;
		HDC screenDC = MyUser32.INSTANCE.GetDC(null);
		HDC memDC = GDI32.INSTANCE.CreateCompatibleDC(screenDC);

		BITMAPINFO bmi = new BITMAPINFO();
		bmi.bmiHeader.biSize = bmi.bmiHeader.size();
		bmi.bmiHeader.biWidth = size;
		bmi.bmiHeader.biHeight = -size;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

		PointerByReference bitsRef = new PointerByReference();
		HBITMAP hBitmap = GDI32.INSTANCE.CreateDIBSection(memDC, bmi, WinGDI.DIB_RGB_COLORS, bitsRef, null, 0);

		com.sun.jna.platform.win32.WinNT.HANDLE oldObj = GDI32.INSTANCE.SelectObject(memDC, hBitmap);

		int DI_NORMAL = 0x0003;
		MyUser32.INSTANCE.DrawIconEx(memDC, 0, 0, hIcon, size, size, 0, null, DI_NORMAL);

		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Pointer bits = bitsRef.getValue();
		if (bits != null) {
			int[] pixels = bits.getIntArray(0, size * size);
			image.setRGB(0, 0, size, size, pixels, 0, size);
		}

		GDI32.INSTANCE.SelectObject(memDC, oldObj);
		GDI32.INSTANCE.DeleteObject(hBitmap);
		GDI32.INSTANCE.DeleteDC(memDC);
		MyUser32.INSTANCE.ReleaseDC(null, screenDC);

		return image;
	}
}