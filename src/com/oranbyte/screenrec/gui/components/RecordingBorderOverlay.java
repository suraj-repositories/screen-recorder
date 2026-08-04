package com.oranbyte.screenrec.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.JWindow;

import com.oranbyte.screenrec.constants.AppColors;

public class RecordingBorderOverlay extends JWindow {

	private static final long serialVersionUID = 1L;
	private static final int BORDER_SIZE = 1;

	public RecordingBorderOverlay(Rectangle captureArea) {

		setBounds(captureArea.x - BORDER_SIZE, captureArea.y - BORDER_SIZE, captureArea.width + BORDER_SIZE * 2,
				captureArea.height + BORDER_SIZE * 2);
		setAlwaysOnTop(true);
		setFocusableWindowState(false);

		setBackground(new Color(0, 0, 0, 0));
		JPanel panel = new JPanel() {

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				Graphics2D g2 = (Graphics2D) g.create();

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				float[] dash = { 6f, 4f };

				g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0));

				g2.setColor(AppColors.RECORDING_BORDER_COLOR);

				g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

				g2.dispose();
			}
		};

		panel.setOpaque(false);
		setContentPane(panel);
	}
}