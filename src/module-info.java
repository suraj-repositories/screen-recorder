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
	requires org.json;
	requires java.net.http;
	requires jdk.httpserver;
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.media;
	requires javafx.swing;
	requires org.bouncycastle.provider;
	requires org.bouncycastle.pkix;

	opens com.oranbyte.screenrec.util to com.sun.jna;
	opens com.oranbyte.screenrec.gui to javafx.fxml;

	exports com.oranbyte.screenrec.util;
	exports com.oranbyte.screenrec.recorder;
	exports com.oranbyte.screenrec;
	exports com.oranbyte.screenrec.gui;

}