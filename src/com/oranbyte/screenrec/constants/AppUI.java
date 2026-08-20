package com.oranbyte.screenrec.constants;
 
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

public class AppUI {

	public AppUI() {
		UIManager.put("FileChooser.upFolderIcon", Icons.FAVICON.icon(24));

		getSystemUI();
	}

	public static void getSystemUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	public static void getSwingUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {

			e.printStackTrace();
		}
	}

	public static String[] getUIList() {
		LookAndFeelInfo[] auxiliaryLookAndFeels = UIManager.getInstalledLookAndFeels();
		String[] answer = null;

		if (auxiliaryLookAndFeels != null) {
			answer = new String[auxiliaryLookAndFeels.length];
			for (int i = 0; i < auxiliaryLookAndFeels.length; i++) {
				answer[i] = auxiliaryLookAndFeels[i].getClassName();
			}
		}
		return answer;
	}

	public static void setLookAndFeelByName(String className) {
		try {
			UIManager.setLookAndFeel(className);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

}