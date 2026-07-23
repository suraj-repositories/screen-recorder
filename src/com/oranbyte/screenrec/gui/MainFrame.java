package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.constants.RecordingMode;
import com.oranbyte.screenrec.gui.components.ImageSwitch;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.ToolbarComboBox;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private SelectionFrame selectionFrame;

	public MainFrame() {
		init();
	}

	private void init() {

		setTitle("Screen Recorder");
		setIconImage(Icons.FAVICON.icon().getImage());
		setAlwaysOnTop(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		setSize(600, 300);
		setLocationRelativeTo(null);

		JToolBar toolbar = initToolbar();

		add(toolbar, BorderLayout.NORTH);

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);

		JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JLabel titleLabel = new JLabel("No Recording Active");
		titleLabel.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 24f));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		titleLabel.setForeground(AppColors.TEXT);

		JLabel subtitleLabel = new JLabel("Select a capture mode and click Start to begin recording.");
		subtitleLabel.setFont(AppConstant.APP_FONT.deriveFont(14f));
		subtitleLabel.setForeground(AppColors.TEXT_SECONDARY);
		subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		content.add(titleLabel);
		content.add(Box.createVerticalStrut(8));
		content.add(subtitleLabel);

		panel.add(content);

		add(panel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(() -> {
			selectionFrame = new SelectionFrame();
			selectionFrame.setVisible(false);
		});

		setVisible(true);
	}

	private JToolBar initToolbar() {

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(AppColors.BACKGROUND);
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

		ToolbarButton newButton = new ToolbarButton("New", Icons.PLUS);

		ToolbarComboBox<CaptureMode> captureMode = new ToolbarComboBox<>(CaptureMode.values());

		ImageSwitch modeSwitch = new ImageSwitch(Icons.CAMERA.icon(24), Icons.VIDEO.icon(24));

		newButton.addActionListener(e -> {

			setVisible(false);

			Timer timer = new Timer(300, ev -> {

				if (selectionFrame == null) {
					selectionFrame = new SelectionFrame();
				}

				CaptureMode mode = (CaptureMode) captureMode.getSelectedItem();
				RecordingMode rMode = modeSwitch.getRecordingMode();
				selectionFrame.activate(this);
				selectionFrame.setCaptureMode(mode);
				selectionFrame.setRecordingMode(rMode);

			});

			timer.setRepeats(false);
			timer.start();

		});

		toolbar.add(newButton);
		toolbar.add(Box.createHorizontalStrut(10));
		toolbar.add(modeSwitch);
		toolbar.add(Box.createHorizontalStrut(10));
		toolbar.add(captureMode);

		return toolbar;
	}

}