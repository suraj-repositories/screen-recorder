package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.Icons;

public class ToolbarButton extends JButton {

	private static final Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 16);

	private Insets padding = new Insets(7, 11, 7, 11);

	private Border createBorder(Color color) {
		return BorderFactory.createCompoundBorder(new LineBorder(color, 1, true), new EmptyBorder(padding));
	}

	public ToolbarButton(String text, Icons icon) {
		this(text, icon, 32);
	}

	public ToolbarButton(String text, Icons icon, int iconSize) {
		super(text, icon.icon(iconSize));
		initialize();
	}

	public ToolbarButton(Icons icon) {
		this("", icon);
	}

	public ToolbarButton(String text) {
		super(text);
		initialize();
	}

	private void initialize() {

		setFocusable(false);
		setFocusPainted(false);

		setContentAreaFilled(false);
		setOpaque(true);

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		setBackground(AppColors.BUTTON);
		setForeground(AppColors.TEXT);
		setFont(DEFAULT_FONT);

		setHorizontalAlignment(LEFT);
		setHorizontalTextPosition(RIGHT);

		setVerticalAlignment(CENTER);
		setVerticalTextPosition(CENTER);

		setIconTextGap(8);

		setBorder(createBorder(AppColors.BORDER));

		installHoverEffects();
	}

	private void installHoverEffects() {

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				setBackground(AppColors.BUTTON_HOVER);
				setBorder(createBorder(AppColors.BORDER_HOVER));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setBackground(AppColors.BUTTON);
				setBorder(createBorder(AppColors.BORDER));
			}

			@Override
			public void mousePressed(MouseEvent e) {
				setBackground(AppColors.BUTTON_PRESSED);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (contains(e.getPoint())) {
					setBackground(AppColors.BUTTON_HOVER);
				} else {
					setBackground(AppColors.BUTTON);
				}
			}
		});
	}

	/* ---------- Helper Methods ---------- */

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

		boolean hovered = getModel().isRollover();

		setBorder(createBorder(hovered ? AppColors.BORDER_HOVER : AppColors.BORDER));

		revalidate();
		repaint();

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
}