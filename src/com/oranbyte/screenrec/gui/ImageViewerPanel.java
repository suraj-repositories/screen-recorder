package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.oranbyte.screenrec.gui.components.ModernScrollPane;
import com.oranbyte.screenrec.gui.components.ZoomImagePanel;

public class ImageViewerPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final ZoomImagePanel imagePanel;
	private final JScrollPane scrollPane;

	private double zoom = 1.0;

	private static final double MIN_ZOOM = 0.1;
	private static final double MAX_ZOOM = 10.0;
	private static final double ZOOM_FACTOR = 1.1;

	private static final int pad = 10;

	public ImageViewerPanel() {

		super(new BorderLayout());

		imagePanel = new ZoomImagePanel();

		scrollPane = new ModernScrollPane(imagePanel);

		scrollPane.setBorder(null);

		scrollPane.getViewport().setBackground(getBackground());

		add(scrollPane, BorderLayout.CENTER);

		installListeners();
	}

	private void installListeners() {

		imagePanel.addMouseWheelListener(this::handleZoom);

		scrollPane.getViewport().addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				centerImage();
			}
		});
	}

	private void handleZoom(MouseWheelEvent e) {

		if (imagePanel.getImage() == null)
			return;

		JViewport viewport = scrollPane.getViewport();

		Point mouse = SwingUtilities.convertPoint(imagePanel, e.getPoint(), viewport);

		Point view = viewport.getViewPosition();

		double oldZoom = zoom;

		if (e.getWheelRotation() < 0)
			zoom = Math.min(MAX_ZOOM, zoom * ZOOM_FACTOR);
		else
			zoom = Math.max(MIN_ZOOM, zoom / ZOOM_FACTOR);

		imagePanel.setZoom(zoom);

		double scale = zoom / oldZoom;

		int newX = (int) ((view.x + mouse.x) * scale - mouse.x);
		int newY = (int) ((view.y + mouse.y) * scale - mouse.y);

		Dimension size = imagePanel.getPreferredSize();

		Rectangle viewRect = viewport.getViewRect();

		newX = Math.max(0, Math.min(newX, size.width - viewRect.width));
		newY = Math.max(0, Math.min(newY, size.height - viewRect.height));

		viewport.setViewPosition(new Point(newX, newY));

		centerImage();
	}

	private void centerImage() {

		Dimension imageSize = imagePanel.getPreferredSize();
		Dimension viewportSize = scrollPane.getViewport().getExtentSize();

		int width = Math.max(imageSize.width, viewportSize.width);
		int height = Math.max(imageSize.height, viewportSize.height);

		imagePanel.setPreferredSize(new Dimension(width, height));

		imagePanel.setImageLocation(Math.max(0, (width - imageSize.width) / 2),
				Math.max(0, (height - imageSize.height) / 2));

		imagePanel.revalidate();
		imagePanel.repaint();
	}

	public void setImage(BufferedImage image) {

		zoom = 1.0;

		imagePanel.setImage(image);
		imagePanel.setZoom(zoom);

		centerImage();
	}

	public BufferedImage getImage() {
		return imagePanel.getImage();
	}

	public double getZoom() {
		return zoom;
	}

	public void setZoom(double zoom) {

		this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

		imagePanel.setZoom(this.zoom);

		centerImage();
	}

	public void zoomIn() {
		setZoom(zoom * ZOOM_FACTOR);
	}

	public void zoomOut() {
		setZoom(zoom / ZOOM_FACTOR);
	}

	public void fitToWidth() {

		if (imagePanel.getImage() == null)
			return;

		int width = scrollPane.getViewport().getWidth();

		setZoom(width / (double) imagePanel.getImage().getWidth());
	}

	public void fitToHeight() {

		if (imagePanel.getImage() == null)
			return;

		int height = scrollPane.getViewport().getHeight();

		setZoom(height / (double) imagePanel.getImage().getHeight());
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}
}