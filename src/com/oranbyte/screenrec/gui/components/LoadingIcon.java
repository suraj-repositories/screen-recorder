package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

public class LoadingIcon implements Icon {

	private final int size;
	private final int strokeWidth;
	private final Color color;

	private int angle;

	public LoadingIcon(int size, int strokeWidth, Color color) {
		this.size = size;
		this.strokeWidth = strokeWidth;
		this.color = color;
	}

	public void rotate() {
		angle = (angle + 30) % 360;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();

		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(color);
			g2.setStroke(new java.awt.BasicStroke(strokeWidth, java.awt.BasicStroke.CAP_ROUND,
					java.awt.BasicStroke.JOIN_ROUND));

			int padding = strokeWidth;
			int diameter = size - padding * 2;

			g2.rotate(Math.toRadians(angle), x + size / 2.0, y + size / 2.0);

			g2.drawArc(x + padding, y + padding, diameter, diameter, 45, 270);

		} finally {
			g2.dispose();
		}
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}
}