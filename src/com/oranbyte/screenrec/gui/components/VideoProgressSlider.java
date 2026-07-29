package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import com.oranbyte.screenrec.constants.AppColors;

public class VideoProgressSlider extends JSlider {

	private static final long serialVersionUID = 1L;

	private Color trackColor = AppColors.GRAY_400;
	private Color progressColor = AppColors.PRIMARY_HOVER;
	private Color thumbColor = AppColors.PRIMARY;

	public VideoProgressSlider() {
		super(0, 100, 0);

		setOpaque(false);
		setFocusable(false);

		setUI(new BasicSliderUI(this) {

			@Override
			public void paintTrack(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				int y = trackRect.y + (trackRect.height / 2) - 3;

				g2.setColor(trackColor);
				g2.fillRoundRect(trackRect.x, y, trackRect.width, 6, 6, 6);

				int progressWidth = (int) ((getValue() / 100.0) * trackRect.width);

				g2.setColor(progressColor);
				g2.fillRoundRect(trackRect.x, y, progressWidth, 6, 6, 6);

				g2.dispose();
			}

			@Override
			public void paintThumb(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();

				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				int outerSize = 20;
				int innerSize = 10;

				int x = thumbRect.x + (thumbRect.width - outerSize) / 2;
				int y = thumbRect.y + (thumbRect.height - outerSize) / 2;

				g2.setColor(thumbColor);
				g2.fillOval(x, y, outerSize, outerSize);

				int innerX = x + (outerSize - innerSize) / 2;
				int innerY = y + (outerSize - innerSize) / 2;

				g2.setColor(Color.WHITE);
				g2.fillOval(innerX, innerY, innerSize, innerSize);

				g2.dispose();
			}

			@Override
			protected Dimension getThumbSize() {
				return new Dimension(20, 20);
			}
		});
	}

	public void setProgressColor(Color color) {
		this.progressColor = color;
		repaint();
	}

	public void setTrackColor(Color color) {
		this.trackColor = color;
		repaint();
	}

	public void setThumbColor(Color color) {
		this.thumbColor = color;
		repaint();
	}
}