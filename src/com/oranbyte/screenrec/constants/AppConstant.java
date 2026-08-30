package com.oranbyte.screenrec.constants;

import java.awt.Font;
import java.nio.file.Paths;

public class AppConstant {

	public static final String APP_NAME = "Screen Recorder";

	public static final String SAVE_LOCATION_SCREENSHOT = Paths
			.get(System.getProperty("user.home"), "Pictures", "Screenshots").toString();

	public static final String SAVE_LOCATION_RECORDING = Paths
			.get(System.getProperty("user.home"), "Videos", "Screen Recordings").toString();

	public static final Font APP_FONT = new Font("Arial", Font.PLAIN, 16);

	public static final int FPS = 50;
	public static final int NEARBY_SCAN_TIMEOUT = 5;

	public static final String SNORE_TOAST_PATH = Paths.get("lib", "snoretoast.exe").toAbsolutePath().toString();
}
