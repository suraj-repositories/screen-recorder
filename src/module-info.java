/**
 * 
 */
/**
 * 
 */
module ScreenRecorder {
	requires java.desktop;
	requires xuggle.xuggler;
	requires com.sun.jna;
	requires com.sun.jna.platform;
	requires uk.co.caprica.vlcj;
	requires uk.co.caprica.vlcj.natives;

	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.media;
	requires javafx.swing;

	opens com.oranbyte.screenrec.util to com.sun.jna;

//
	exports com.oranbyte.screenrec.util;
	exports com.oranbyte.screenrec.recorder;
}