package com.oranbyte.screenrec.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.AbstractBorder;

public class RoundedBorder extends AbstractBorder {

	private static final long serialVersionUID = 1L;

	private final Color color;
	private final int radius;
	private final int thickness;

	public RoundedBorder(Color color, int radius, int thickness) {
		this.color = color;
		this.radius = radius;
		this.thickness = thickness;
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(thickness, thickness, thickness, thickness);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.set(thickness, thickness, thickness, thickness);
		return insets;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(color);
		g2.setStroke(new BasicStroke(thickness));
		g2.drawRoundRect(x + thickness / 2, y + thickness / 2, width - thickness, height - thickness, radius, radius);

		g2.dispose();
	}
}