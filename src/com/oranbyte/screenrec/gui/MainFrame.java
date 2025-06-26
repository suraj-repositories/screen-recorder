package com.oranbyte.screenrec.gui;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import com.oranbyte.screenrec.recorder.ScreenRecorder;

public class MainFrame {
    private final SelectionFrame selectionFrame;
    private final ControlFrame controlFrame;
    private ScreenRecorder recorder;

    public MainFrame() {
        selectionFrame = new SelectionFrame();
        controlFrame = new ControlFrame(this);
    }

   
    
    public void startRecording() {
//        Rectangle captureArea = selectionFrame.getCaptureBounds();
        Rectangle captureArea = ensureEvenDimensions(selectionFrame.drawPanel.selectedRectangle);

        if (captureArea == null || captureArea.width <= 0 || captureArea.height <= 0) {
            JOptionPane.showMessageDialog(null, "Please select a valid area before recording.");
            return;
        }

        selectionFrame.setVisible(false);
        // selectionFrame.dispose(); // Optional if you want to fully release resources

        recorder = new ScreenRecorder(captureArea);
        recorder.start();
    }


    public void stopRecording() {
        if (recorder != null) {
            recorder.stop();
        }
        selectionFrame.setVisible(true);
        selectionFrame.dispose();
    }
    
    public void toggleRecording(JButton button) {
    	if(button.getText().toLowerCase().contains("start")) {
    		button.setText("Stop");
    		button.setBackground(Color.RED);
    		button.setForeground(Color.WHITE);
    		startRecording();
    	}else {
    		button.setText("Start");
    		button.setBackground(Color.GREEN.darker());
            button.setForeground(Color.WHITE);
    		stopRecording();
    	}
    }
    public static Rectangle ensureEvenDimensions(Rectangle rect) {
        int x = rect.x;
        int y = rect.y;
        int width = rect.width % 2 == 0 ? rect.width : rect.width - 1;
        int height = rect.height % 2 == 0 ? rect.height : rect.height - 1;

        return new Rectangle(x, y, width, height);
    }

    
}
