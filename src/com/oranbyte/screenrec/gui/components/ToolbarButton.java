package com.oranbyte.screenrec.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.Icons;

public class ToolbarButton extends JButton {

	private static final long serialVersionUID = 1L;
	private static final Font DEFAULT_FONT = AppConstant.APP_FONT;
	private static final int BORDER_THICKNESS = 1;
	public static final int DEFAULT_ICON_SIZE = 32;

	private Insets padding = new Insets(7, 11, 7, 11);
	private int borderRadius = 10;
	private boolean hasBorder = true;
	private boolean isAllowed = true;

	private Color backgroundColor = new Color(0, 0, 0, 0);  
	private Color foregroundColor = AppColors.TEXT;
	private Color hoverBackgroundColor = AppColors.BUTTON_HOVER;
	private Color pressedBackgroundColor = AppColors.BUTTON_PRESSED;

	private Color currentBorderColor = AppColors.BORDER;

	public ToolbarButton(String text, Icons icon) {
		this(text, icon, DEFAULT_ICON_SIZE);
	}

	public ToolbarButton(String text, Icons icon, int iconSize) {
		super(text, icon.icon(iconSize));
		initialize();
	}

	public ToolbarButton(String text, Icons icon, int iconSize, int borderRadius) {
		this(text, icon, iconSize);
		this.borderRadius = borderRadius;
	}

	public ToolbarButton(Icons icon) {
		this("", icon);
	}

	public ToolbarButton(Icons icon, int iconSize) {
		this("", icon, iconSize);
	}

	public ToolbarButton(String text) {
		super(text);
		initialize();
	}

	private void initialize() {
		setFocusable(false);
		setFocusPainted(false);
		setContentAreaFilled(false);
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setBackground(backgroundColor);
		setForeground(foregroundColor);
		setFont(DEFAULT_FONT);
		setHorizontalAlignment(LEFT);
		setHorizontalTextPosition(RIGHT);
		setVerticalAlignment(CENTER);
		setVerticalTextPosition(CENTER);
		setIconTextGap(8);
		applyBorder(AppColors.BORDER);
		installHoverEffects();
	}

	private Border buildBorder(Color color) {
		Border outerBorder = (hasBorder && color != null)
				? new RoundedBorder(color, borderRadius, BORDER_THICKNESS)
				: new EmptyBorder(BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS, BORDER_THICKNESS);
		Border innerBorder = new EmptyBorder(padding);
		return BorderFactory.createCompoundBorder(outerBorder, innerBorder);
	}

	private void applyBorder(Color color) {
		this.currentBorderColor = color;
		super.setBorder(buildBorder(color));
	}

	private void installHoverEffects() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (hoverBackgroundColor != null) {
					setBackground(hoverBackgroundColor);
				}
				if (hasBorder) {
					applyBorder(AppColors.BORDER_HOVER);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setBackground(backgroundColor != null ? backgroundColor : new Color(0, 0, 0, 0));
				if (hasBorder) {
					applyBorder(AppColors.BORDER);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (pressedBackgroundColor != null) {
					setBackground(pressedBackgroundColor);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				boolean inside = contains(e.getPoint());
				if (inside && hoverBackgroundColor != null) {
					setBackground(hoverBackgroundColor);
				} else {
					setBackground(backgroundColor != null ? backgroundColor : new Color(0, 0, 0, 0));
				}
			}
		});
	}

	public ToolbarButton setIconSize(Icons icon, int size) {
		setIcon(icon.icon(size));
		return this;
	}

	public ToolbarButton setButtonFont(int size) {
		setFont(DEFAULT_FONT.deriveFont((float) size));
		return this;
	}

	public ToolbarButton setGap(int gap) {
		setIconTextGap(gap);
		return this;
	}

	public ToolbarButton setPadding(int top, int left, int bottom, int right) {
		padding = new Insets(top, left, bottom, right);
		applyBorder(currentBorderColor);
		revalidate();
		repaint();
		return this;
	}

	public ToolbarButton setHasBorder(boolean hasBorder) {
		this.hasBorder = hasBorder;
		applyBorder(hasBorder ? currentBorderColor : null);
		return this;
	}

	public ToolbarButton makePrimary() {
		setForeground(AppColors.PRIMARY);
		return this;
	}

	public ToolbarButton makeDanger() {
		setForeground(new Color(0xD32F2F));
		return this;
	}

	public ToolbarButton makeSuccess() {
		setForeground(new Color(0x2E7D32));
		return this;
	}

	@Override
	public void setBorder(Border border) {
		this.hasBorder = (border != null);
		if (border == null) {
			applyBorder(null);
		} else {
			super.setBorder(border);
		}
	}

	public void setBorderRadius(int borderRadius) {
		this.borderRadius = borderRadius;
		applyBorder(hasBorder ? currentBorderColor : null);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Color bg = getBackground();

		// Paint rounded background whenever a non-transparent color is active
		if (bg != null && bg.getAlpha() > 0) {
			Graphics2D g2Bg = (Graphics2D) g.create();
			g2Bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2Bg.setColor(bg);
			g2Bg.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, borderRadius, borderRadius);
			g2Bg.dispose();
		}

		super.paintComponent(g);

		if (!isAllowed) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(AppColors.PRIMARY);
			g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int margin = 8;
			g2.drawLine(margin, getHeight() - margin, getWidth() - margin, margin);
			g2.dispose();
		}
	}

	public boolean isAllowed() {
		return isAllowed;
	}

	public void setAllowed(boolean allowed) {
		if (this.isAllowed != allowed) {
			this.isAllowed = allowed;
			repaint();
		}
	}

	public ToolbarButton setSm() {
		this.setPadding(4, 7, 4, 7);
		return this;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		// Use transparent color instead of null to prevent Swing from breaking background states
		this.backgroundColor = (backgroundColor == null) ? new Color(0, 0, 0, 0) : backgroundColor;
		setBackground(this.backgroundColor);
	}

	public Color getForegroundColor() {
		return foregroundColor;
	}

	public void setForegroundColor(Color foregroundColor) {
		this.foregroundColor = foregroundColor;
		setForeground(foregroundColor);
	}

	public Color getHoverBackgroundColor() {
		return hoverBackgroundColor;
	}

	public void setHoverBackgroundColor(Color hoverBackgroundColor) {
		this.hoverBackgroundColor = hoverBackgroundColor;
	}

	public Color getPressedBackgroundColor() {
		return pressedBackgroundColor;
	}

	public void setPressedBackgroundColor(Color pressedBackgroundColor) {
		this.pressedBackgroundColor = pressedBackgroundColor;
	}
}