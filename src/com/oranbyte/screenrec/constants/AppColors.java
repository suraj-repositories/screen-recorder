package com.oranbyte.screenrec.constants;

import java.awt.Color;

public final class AppColors {

	private AppColors() {
	}

	/* ---------- Theme ---------- */

	public static final Color BACKGROUND = new Color(0xF5F5F5);
	public static final Color SURFACE = Color.WHITE;

	/* ---------- Primary ---------- */

	public static final Color PRIMARY = new Color(0xF57C00);
	public static final Color PRIMARY_HOVER = new Color(0xFB8C00);
	public static final Color PRIMARY_PRESSED = new Color(0xEF6C00);

	/* ---------- Text ---------- */

	public static final Color TEXT = new Color(0x333333);
	public static final Color TEXT_SECONDARY = new Color(0x757575);

	/* ---------- Borders ---------- */

	public static final Color BORDER = new Color(0xD8D8D8);
	public static final Color BORDER_HOVER = PRIMARY;

	/* ---------- Buttons ---------- */

	public static final Color BUTTON = SURFACE;
	public static final Color BUTTON_HOVER = new Color(0xF7F7F7);
	public static final Color BUTTON_PRESSED = new Color(0xECECEC);
}