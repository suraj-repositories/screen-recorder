package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;

@SuppressWarnings("serial")
public class ToolbarComboBox<E> extends JComboBox<E> {

	private static final Font DEFAULT_FONT = AppConstant.APP_FONT;

	private Insets padding = new Insets(7, 11, 7, 11);

	private int borderRadius = 10;

	@SafeVarargs
	public ToolbarComboBox(E... items) {
		super(items);
		initialize();
	}

	private void initialize() {

		setFocusable(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		setBackground(AppColors.BUTTON);
		setForeground(AppColors.TEXT);

		setFont(DEFAULT_FONT);

		setBorder(createBorder(AppColors.BORDER));

		setRenderer(new ComboRenderer());

		setUI(new ComboUI());

		addPopupMenuListener(new javax.swing.event.PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
				setBackground(AppColors.BUTTON_HOVER);
				setBorder(createBorder(AppColors.BORDER_HOVER));
			}

			@Override
			public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
				setBackground(AppColors.BUTTON);
				setBorder(createBorder(AppColors.BORDER));
			}

			@Override
			public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
				setBackground(AppColors.BUTTON);
				setBorder(createBorder(AppColors.BORDER));
			}
		});

		setPreferredSize(new Dimension(180, 42));
	}

	private Border createBorder(Color color) {
		return BorderFactory.createCompoundBorder(new RoundedBorder(color, borderRadius, 1), new EmptyBorder(padding));
	}

	public ToolbarComboBox setPadding(int top, int left, int bottom, int right) {
		padding = new Insets(top, left, bottom, right);
		setBorder(createBorder(AppColors.BORDER));
		repaint();
		return this;
	}

	public ToolbarComboBox setComboFont(int size) {
		setFont(DEFAULT_FONT.deriveFont((float) size));
		return this;
	}

	/**
	 * Custom renderer
	 */
	private class ComboRenderer extends JLabel implements ListCellRenderer<Object> {

		ComboRenderer() {
			setOpaque(true);
			setBorder(new EmptyBorder(7, 11, 7, 11));
			setFont(DEFAULT_FONT);
		}

		@Override
		public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {

			setText(value == null ? "" : value.toString());

			if (isSelected) {
				setBackground(AppColors.BUTTON_HOVER);
			} else {
				setBackground(AppColors.BUTTON);
			}

			setForeground(AppColors.TEXT);

			return this;
		}
	}

	/**
	 * Custom ComboBox UI
	 */
	private class ComboUI extends BasicComboBoxUI {

		@Override
		protected javax.swing.JButton createArrowButton() {

			javax.swing.JButton button = new javax.swing.JButton("▼");

			button.setBorder(BorderFactory.createEmptyBorder());
			button.setFocusable(false);
			button.setContentAreaFilled(false);

			button.setBackground(AppColors.BUTTON);
			button.setForeground(AppColors.TEXT);

			button.setFont(DEFAULT_FONT.deriveFont(Font.BOLD, 12f));

			return button;
		}

		@Override
		protected BasicComboPopup createPopup() {

			BasicComboPopup popup = new BasicComboPopup(comboBox);

			popup.setBorder(new LineBorder(AppColors.BORDER));

			popup.getList().setBackground(AppColors.BUTTON);
			popup.getList().setForeground(AppColors.TEXT);
			popup.getList().setSelectionBackground(AppColors.BUTTON_HOVER);
			popup.getList().setSelectionForeground(AppColors.TEXT);

			return popup;
		}
	}

	public void setBorderRadius(int borderRadius) {
		this.borderRadius = borderRadius;
	}
}