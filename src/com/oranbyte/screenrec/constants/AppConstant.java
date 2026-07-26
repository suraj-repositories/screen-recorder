package com.oranbyte.screenrec.constants;

import java.awt.Font;
import java.nio.file.Paths;

public class AppConstant {

	public static final String APP_NAME = "Screen Recorder";
	public static final String APP_LOGO = "/com/oranbyte/screenrec/icons/favicon.png";
	public static final String SAVE_LOCATION = "C:\\Users\\Shubham\\Desktop";

	public static final Font APP_FONT = new Font("Arial", Font.PLAIN, 16);

	public static final String SNORE_TOAST_PATH = Paths.get("lib", "snoretoast.exe").toAbsolutePath().toString();
}
