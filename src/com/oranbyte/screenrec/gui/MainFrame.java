package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.ToolbarComboBox;
import com.oranbyte.screenrec.recorder.ScreenRecorder;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private SelectionFrame selectionFrame;
	private ControlFrame controlFrame;
	private ScreenRecorder recorder;

	private JButton recordButton;
	private boolean recording = false;

	public MainFrame() {
		init();
	}

	private void init() {

		setTitle("Screen Recorder");
		setIconImage(Icons.FAVICON.icon().getImage());
		setAlwaysOnTop(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		setSize(700, 450);
		setLocationRelativeTo(null);

		add(initToolbar(), BorderLayout.NORTH);

		recordButton = new JButton("Start");
		recordButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
		recordButton.setBackground(Color.GREEN.darker());
		recordButton.setForeground(Color.WHITE);
		recordButton.setFocusable(false);
		recordButton.addActionListener(e -> toggleRecording());

		add(recordButton, BorderLayout.CENTER);

		setVisible(true);
	}

	private JToolBar initToolbar() {

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(AppColors.BACKGROUND);
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

		ToolbarButton newButton = new ToolbarButton("New", Icons.PLUS);

		newButton.addActionListener(e -> {

			setVisible(false);

			if (selectionFrame != null) {
				selectionFrame.setVisible(false);
			}

			if (controlFrame != null) {
				controlFrame.setVisible(false);
			}

			Timer timer = new Timer(300, ev -> {

				if (selectionFrame == null) {
					selectionFrame = new SelectionFrame();
				} else {
					selectionFrame.refreshScreen();
					selectionFrame.setVisible(true);
				}

				if (controlFrame == null) {
					controlFrame = new ControlFrame(selectionFrame, this, selectionFrame);
				}

				controlFrame.setVisible(true);
				controlFrame.toFront();
				controlFrame.requestFocus();

			});

			timer.setRepeats(false);
			timer.start();
		});

		toolbar.add(newButton);

		toolbar.add(Box.createHorizontalStrut(10));

		ToolbarComboBox captureMode = new ToolbarComboBox("Rectangle", "Entire Screen");

		toolbar.add(captureMode);

		return toolbar;
	}

	public void startRecording() {

		if (selectionFrame == null || selectionFrame.drawPanel == null
				|| selectionFrame.drawPanel.selectedRectangle == null) {

			JOptionPane.showMessageDialog(this, "Please create a selection first.");
			return;
		}

		Rectangle captureArea = ensureEvenDimensions(selectionFrame.drawPanel.selectedRectangle);

		if (captureArea.width <= 0 || captureArea.height <= 0) {

			JOptionPane.showMessageDialog(this, "Please select a valid recording area.");
			return;
		}

		selectionFrame.setVisible(false);

		recorder = new ScreenRecorder(captureArea);
		recorder.start();
	}

	public void stopRecording() {

		if (recorder != null) {
			recorder.stop();
			recorder = null;
		}

		if (selectionFrame != null) {
			selectionFrame.dispose();
			selectionFrame = null;
		}

		if (controlFrame != null) {
			controlFrame.dispose();
			controlFrame = null;
		}
	}

	private void toggleRecording() {

		if (!recording) {

			startRecording();

			if (recorder != null) {
				recording = true;
				recordButton.setText("Stop");
				recordButton.setBackground(Color.RED);
			}

		} else {

			stopRecording();

			recording = false;
			recordButton.setText("Start");
			recordButton.setBackground(Color.GREEN.darker());
		}
	}

	public static Rectangle ensureEvenDimensions(Rectangle rect) {

		if (rect == null) {
			return null;
		}

		int width = rect.width;
		int height = rect.height;

		if ((width & 1) == 1) {
			width--;
		}

		if ((height & 1) == 1) {
			height--;
		}

		return new Rectangle(rect.x, rect.y, width, height);
	}

}