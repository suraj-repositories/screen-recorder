package com.oranbyte.screenrec.gui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class ZoomImagePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private BufferedImage image;

	private double zoom = 1.0;

	private int imageX;
	private int imageY;

	public ZoomImagePanel() {
		setOpaque(true);
	}

	public void setImage(BufferedImage image) {
		this.image = image;
		revalidate();
		repaint();
	}

	public BufferedImage getImage() {
		return image;
	}

	public void setZoom(double zoom) {
		this.zoom = zoom;
		revalidate();
		repaint();
	}

	public double getZoom() {
		return zoom;
	}

	public void setImageLocation(int x, int y) {
		this.imageX = x;
		this.imageY = y;
		repaint();
	}

	public int getImageX() {
		return imageX;
	}

	public int getImageY() {
		return imageY;
	}

	public int getScaledImageWidth() {
		if (image == null)
			return 0;

		return (int) Math.round(image.getWidth() * zoom);
	}

	public int getScaledImageHeight() {
		if (image == null)
			return 0;

		return (int) Math.round(image.getHeight() * zoom);
	}

	@Override
	public Dimension getPreferredSize() {

		if (image == null)
			return new Dimension();

		return new Dimension(getScaledImageWidth(), getScaledImageHeight());
	}

	@Override
	protected void paintComponent(Graphics g) {

		super.paintComponent(g);

		if (image == null)
			return;

		Graphics2D g2 = (Graphics2D) g.create();

		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.drawImage(image, imageX, imageY, getScaledImageWidth(), getScaledImageHeight(), null);

		g2.dispose();
	}
}