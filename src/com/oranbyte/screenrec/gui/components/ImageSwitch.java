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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.RecordingMode;

public class ImageSwitch extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private RecordingMode recordingMode = RecordingMode.SCREENSHOT;
	private final Icon[] icons;

	private Color background = AppColors.BUTTON;

	private float thumbX = 2;
	private Timer animation;

	private final EventListenerList listenerList = new EventListenerList();

	public ImageSwitch(Icon left, Icon right) {
		this.icons = new Icon[] { left, right };

		setPreferredSize(new Dimension(90, 45));
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

				RecordingMode mode = e.getX() < half ? RecordingMode.SCREENSHOT : RecordingMode.VIDEO;

				if (recordingMode != mode) {
					animateTo(mode);
				} else {
					repaint();
				}
			}
		};

		addMouseListener(adapter);
	}

	private void animateTo(RecordingMode mode) {

		recordingMode = mode;
		fireStateChanged();

		int half = getWidth() / 2;
		final float target = mode == RecordingMode.SCREENSHOT ? 2 : half;

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

	public RecordingMode getRecordingMode() {
		return recordingMode;
	}

	public void setRecordingMode(RecordingMode mode) {
		if (recordingMode != mode) {
			animateTo(mode);
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

	public void addChangeListener(ChangeListener listener) {
		listenerList.add(ChangeListener.class, listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}

	protected void fireStateChanged() {
		ChangeEvent event = new ChangeEvent(this);
		for (ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
			listener.stateChanged(event);
		}
	}
}