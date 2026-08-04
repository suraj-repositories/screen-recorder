package com.oranbyte.screenrec.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;

public class CountdownOverlay extends JWindow {

	private static final long serialVersionUID = 1L;

	private int count = 3;

	public CountdownOverlay(Rectangle area) {
		setBounds(area);
		setBackground(new Color(0, 0, 0, 0));
		setAlwaysOnTop(true);
	}

	public void startCountdown(Runnable finished) {

		setVisible(true);

		Timer timer = new Timer(1000, null);

		timer.addActionListener(e -> {

			repaint();

			count--;

			if (count == 0) {

				timer.stop();
				dispose();

				SwingUtilities.invokeLater(finished);
			}

		});

		repaint();
		timer.start();
	}

	@Override
	public void paint(Graphics g) {

		Graphics2D g2 = (Graphics2D) g.create();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setBackground(new Color(0, 0, 0, 0));
		g2.clearRect(0, 0, getWidth(), getHeight());

		g2.setStroke(new BasicStroke(1f));

		float[] dash = { 6f, 4f };
		g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0));

		g2.setColor(AppColors.BORDER);
		g2.drawRect(2, 2, getWidth() - 5, getHeight() - 5);

		int boxSize = 100;
		int boxX = (getWidth() - boxSize) / 2;
		int boxY = (getHeight() - boxSize) / 2;
		int cornerRadius = 20;

		g2.setColor(new Color(0, 0, 0, 180));
		g2.fillRoundRect(boxX, boxY, boxSize, boxSize, cornerRadius, cornerRadius);

		g2.setFont(getFont().deriveFont(Font.BOLD, 65f));
		String text = String.valueOf(count);

		FontMetrics fm = g2.getFontMetrics();
		int x = boxX + (boxSize - fm.stringWidth(text)) / 2;
		int y = boxY + ((boxSize - fm.getHeight()) / 2) + fm.getAscent();

		g2.setColor(Color.WHITE);
		g2.drawString(text, x, y);

		g2.dispose();
	}
}