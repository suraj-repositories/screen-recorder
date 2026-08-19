package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import com.oranbyte.screenrec.constants.AppColors;

public class VideoProgressSlider extends JSlider {

	private static final long serialVersionUID = 1L;

	private Color trackColor = AppColors.GRAY_400;
	private Color progressColor = AppColors.PRIMARY_HOVER;
	private Color thumbColor = AppColors.PRIMARY;

	private double totalSeconds = 0;

	private boolean hovering = false;
	private int hoverX = -1;

	private VideoSliderUI sliderUI;

	public VideoProgressSlider() {
		super(0, 100, 0);
		setOpaque(false);
		setFocusable(false);

		sliderUI = new VideoSliderUI(this);
		setUI(sliderUI);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				hovering = true;
				hoverX = e.getX();
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				hovering = false;
				hoverX = -1;
				repaint();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				hoverX = e.getX();
				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				hoverX = e.getX();
				repaint();
			}
		});
	}

	public void setTotalSeconds(double totalSeconds) {
		this.totalSeconds = Math.max(0, totalSeconds);
	}

	private String formatHoverTime(double seconds) {
		int total = (int) Math.round(Math.max(0, seconds));
		int min = total / 60;
		int sec = total % 60;
		return String.format("%02d:%02d", min, sec);
	}

	private class VideoSliderUI extends BasicSliderUI {

		VideoSliderUI(JSlider slider) {
			super(slider);
		}

		@Override
		protected TrackListener createTrackListener(JSlider slider) {
			return new TrackListener() {

				@Override
				public void mousePressed(MouseEvent e) {
					if (!slider.isEnabled())
						return;
					slider.requestFocus();
					slider.setValueIsAdjusting(true);
					updateValueFromMouse(e);
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					if (!slider.isEnabled())
						return;
					updateValueFromMouse(e);
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (!slider.isEnabled())
						return;
					updateValueFromMouse(e);
					slider.setValueIsAdjusting(false);
				}

				private void updateValueFromMouse(MouseEvent e) {
					if (slider.getOrientation() == JSlider.HORIZONTAL) {
						slider.setValue(valueForXPosition(e.getX()));
					} else {
						slider.setValue(valueForYPosition(e.getY()));
					}
				}
			};
		}

		@Override
		public void paint(Graphics g, javax.swing.JComponent c) {
			super.paint(g, c);
			if (hovering && hoverX >= 0 && totalSeconds > 0) {
				paintHoverBubble(g);
			}
		}

		@Override
		public void paintTrack(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int y = trackRect.y + (trackRect.height / 2) - 3;
			g2.setColor(trackColor);
			g2.fillRoundRect(trackRect.x, y, trackRect.width, 6, 6, 6);
			int progressWidth = (int) (((getValue() - getMinimum()) / (double) (getMaximum() - getMinimum()))
					* trackRect.width);
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

		private void paintHoverBubble(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int clampedX = Math.max(trackRect.x, Math.min(trackRect.x + trackRect.width, hoverX));
			int value = valueForXPosition(clampedX);
			double ratio = (value - slider.getMinimum()) / (double) (slider.getMaximum() - slider.getMinimum());
			String text = formatHoverTime(ratio * totalSeconds);

			FontMetrics fm = g2.getFontMetrics(slider.getFont());
			int textWidth = fm.stringWidth(text);
			int bubbleWidth = textWidth + 16;
			int bubbleHeight = 22;

			int bx = clampedX - bubbleWidth / 2;
			bx = Math.max(0, Math.min(slider.getWidth() - bubbleWidth, bx));
			int by = trackRect.y - bubbleHeight - 8;

			g2.setColor(new Color(20, 20, 20, 220));
			g2.fillRoundRect(bx, by, bubbleWidth, bubbleHeight, 8, 8);

			g2.setColor(Color.WHITE);
			g2.setFont(slider.getFont());
			int tx = bx + (bubbleWidth - textWidth) / 2;
			int ty = by + (bubbleHeight + fm.getAscent()) / 2 - 2;
			g2.drawString(text, tx, ty);

			g2.dispose();
		}
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