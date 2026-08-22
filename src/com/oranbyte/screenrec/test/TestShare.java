package com.oranbyte.screenrec.test;
 
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.constants.AppUI;
import com.oranbyte.screenrec.gui.ShareDialog;
 

public class TestShare extends JFrame {
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TestShare(File file) {
         
    	new AppUI();
    	
    	
    	
		JButton button = new JButton("click me");
		button.addActionListener(e -> {
			if (file == null)
				return;

			ShareDialog shareDialog = new ShareDialog(this, file);
			shareDialog.setVisible(true);

		});
    	
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        add(button);
        
        setSize(1000, 650);
        setLocationRelativeTo(null);
 
    }

    public static void main(String[] args) { 
    	
    	
    	
        SwingUtilities.invokeLater(() -> {
            String videoPath = "C:\\Users\\Shubham\\Videos\\REACT AUTH\\2- React User Login and Authentication with Axios_720p.mp4";

            
            String filePath = "C:\\Users\\Shubham\\Desktop\\sql.png";
            TestShare frame = new TestShare(new File(videoPath));
            frame.setVisible(true);
        });
    }
}