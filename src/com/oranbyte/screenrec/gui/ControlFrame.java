package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.ToolbarComboBox;

public class ControlFrame extends JFrame {
	public ControlFrame(MainFrame mainFrame) {
		setTitle("Controls");
		setIconImage(Icons.FAVICON.icon().getImage());
		setAlwaysOnTop(true);
		setSize(600, 400);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JButton button = new JButton("Start");
		button.setFont(new Font("Segoe UI", Font.BOLD, 14));
		button.setBackground(Color.GREEN.darker());
		button.setForeground(Color.WHITE);
		button.setFocusable(false);
		button.addActionListener(e -> mainFrame.toggleRecording(button));

		JToolBar toolbar = this.initToolbar();

		add(button);
		add(toolbar, BorderLayout.PAGE_START);
		setVisible(true);
	}

	public JToolBar initToolbar() {
		JToolBar toolbar = new JToolBar("Applications");
		toolbar.setBackground(AppColors.BACKGROUND);
		toolbar.setFloatable(false);
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

		ToolbarButton newBtn = new ToolbarButton("New", Icons.PLUS);
		toolbar.add(newBtn);

		toolbar.add(Box.createHorizontalStrut(10));

		ToolbarComboBox captureMode = new ToolbarComboBox("Rectangle", "Frame Size");

		toolbar.add(captureMode);

		return toolbar;
	}
}
