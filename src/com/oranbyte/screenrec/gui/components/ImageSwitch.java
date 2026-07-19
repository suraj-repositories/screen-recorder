package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;

public class ImageSwitch extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int selectedIndex;

	private final Icon[] icons;

	private Color background = AppColors.BUTTON;

	private float thumbX = 2;
	private Timer animation;

	public ImageSwitch(Icon left, Icon right) {
		this.icons = new Icon[] { left, right };

		setPreferredSize(new Dimension(40, 36));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		MouseAdapter adapter = new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				background = AppColors.BUTTON_HOVER;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				background = AppColors.BUTTON;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				background = AppColors.BUTTON_PRESSED;
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {

				background = contains(e.getPoint()) ? AppColors.BUTTON_HOVER : AppColors.BUTTON;

				int half = getWidth() / 2;
				int index = e.getX() < half ? 0 : 1;

				if (selectedIndex != index) {
					animateTo(index);
				} else {
					repaint();
				}
			}
		};

		addMouseListener(adapter);
	}

	private void animateTo(int index) {

		selectedIndex = index;

		int half = getWidth() / 2;
		final float target = index == 0 ? 2 : half;

		if (animation != null && animation.isRunning()) {
			animation.stop();
		}

		animation = new Timer(200 / 60, e -> {

			thumbX += (target - thumbX) * 0.18f;

			if (Math.abs(target - thumbX) < 0.5f) {
				thumbX = target;
				animation.stop();
			}

			repaint();
		});

		animation.start();
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public void setSelectedIndex(int index) {
		if (selectedIndex != index) {
			animateTo(index);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {

		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g.create();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();
		int half = w / 2;

		g2.setColor(background);
		g2.fillRoundRect(0, 0, w, h, 14, 14);

		g2.setColor(AppColors.BORDER);
		g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

		g2.setColor(AppColors.PRIMARY);
		g2.fillRoundRect(Math.round(thumbX), 2, half - 2, h - 4, 12, 12);

		for (int i = 0; i < 2; i++) {

			Icon icon = icons[i];

			int x = i * half + (half - icon.getIconWidth()) / 2;
			int y = (h - icon.getIconHeight()) / 2;

			icon.paintIcon(this, g2, x, y);
		}

		g2.dispose();
	}
}