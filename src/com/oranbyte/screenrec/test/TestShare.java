package com.oranbyte.screenrec.test;

import java.io.File;

import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.constants.AppUI;
import com.oranbyte.screenrec.gui.ShareDialog;

public class TestShare {

	public TestShare(File file) {

		new AppUI();

		ShareDialog shareDialog = new ShareDialog(null, file);
		shareDialog.setVisible(true);
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {

			boolean vid = false;

			String videoPath = "C:\\Users\\Shubham\\Videos\\REACT AUTH\\2- React User Login and Authentication with Axios_720p.mp4";
			String filePath = "C:\\Users\\Shubham\\Desktop\\sql.png";

			new TestShare(new File(vid ? videoPath : filePath));

		});
	}
}