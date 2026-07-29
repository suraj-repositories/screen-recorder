package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class ModernScrollPane extends JScrollPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ModernScrollPane(JComponent view) {
		super(view);

		setBorder(BorderFactory.createEmptyBorder());
		setViewportBorder(BorderFactory.createEmptyBorder());
		getViewport().setBackground(Color.WHITE);

		setHorizontalScrollBar(createModernScrollBar(JScrollBar.HORIZONTAL));
		setVerticalScrollBar(createModernScrollBar(JScrollBar.VERTICAL));

		getHorizontalScrollBar().setUnitIncrement(20);
		getVerticalScrollBar().setUnitIncrement(20);

		setWheelScrollingEnabled(true);
	}

	private JScrollBar createModernScrollBar(int orientation) {
		JScrollBar bar = new JScrollBar(orientation);
		bar.setPreferredSize(orientation == JScrollBar.VERTICAL ? new Dimension(8, 0) : new Dimension(0, 8));
		bar.setUI(new ModernScrollBarUI());
		bar.setOpaque(false);
		return bar;
	}

	private static class ModernScrollBarUI extends BasicScrollBarUI {

		private static final Color THUMB = new Color(170, 170, 170);
		private static final Color THUMB_HOVER = new Color(140, 140, 140);

		private boolean hover = false;

		@Override
		protected void configureScrollBarColors() {
			thumbColor = THUMB;
			trackColor = new Color(0, 0, 0, 0);
		}

		@Override
		protected JButton createDecreaseButton(int orientation) {
			return createZeroButton();
		}

		@Override
		protected JButton createIncreaseButton(int orientation) {
			return createZeroButton();
		}

		private JButton createZeroButton() {
			JButton b = new JButton();
			b.setPreferredSize(new Dimension(0, 0));
			b.setMinimumSize(new Dimension(0, 0));
			b.setMaximumSize(new Dimension(0, 0));
			b.setBorder(null);
			b.setOpaque(false);
			b.setContentAreaFilled(false);
			return b;
		}

		@Override
		protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle trackBounds) {
			// Transparent track
		}

		@Override
		protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle thumbBounds) {

			if (!scrollbar.isEnabled() || thumbBounds.width <= 0 || thumbBounds.height <= 0)
				return;

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(hover ? THUMB_HOVER : THUMB);

			int arc = 8;
			int margin = 2;

			if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
				g2.fillRoundRect(thumbBounds.x + margin, thumbBounds.y, thumbBounds.width - margin * 2,
						thumbBounds.height, arc, arc);
			} else {
				g2.fillRoundRect(thumbBounds.x, thumbBounds.y + margin, thumbBounds.width,
						thumbBounds.height - margin * 2, arc, arc);
			}

			g2.dispose();
		}

		@Override
		protected void setThumbBounds(int x, int y, int width, int height) {
			super.setThumbBounds(x, y, width, height);
			scrollbar.repaint();
		}

		@Override
		protected ScrollListener createScrollListener() {
			return super.createScrollListener();
		}

		@Override
		protected void installListeners() {
			super.installListeners();

			scrollbar.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseEntered(java.awt.event.MouseEvent e) {
					hover = true;
					scrollbar.repaint();
				}

				@Override
				public void mouseExited(java.awt.event.MouseEvent e) {
					hover = false;
					scrollbar.repaint();
				}
			});
		}
	}
}