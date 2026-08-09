package com.oranbyte.screenrec.gui.components;

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;

public class SizeLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private final int radius = 10;

	private float alpha = 1.0f;
	private Timer fadeTimer;
	private static final int ANIMATION_DURATION_MS = 200;
	private static final int TIMER_DELAY_MS = 15;

	public SizeLabel() {
		initUI();
	}

	public SizeLabel(String text) {
		super(text);
		initUI();
	}

	public void initUI() {
		setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD));
		setHorizontalAlignment(SwingConstants.CENTER);
		setOpaque(false);
		setForeground(AppColors.TEXT);
		setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
	}
 
	public void showInstantly() {
		if (fadeTimer != null && fadeTimer.isRunning()) {
			fadeTimer.stop();
		}
		alpha = 1.0f;
		super.setVisible(true);
		repaint();
	}
 
	public void hideAnimated() {
		startFadeOut();
	}
 
	public void hideInstantly() {
		if (fadeTimer != null && fadeTimer.isRunning()) {
			fadeTimer.stop();
		}
		alpha = 0.0f;
		super.setVisible(false);
		repaint();
	}

	private void startFadeOut() {
		if (fadeTimer != null && fadeTimer.isRunning()) {
			fadeTimer.stop();
		}
 
		if (!isVisible() || alpha <= 0.0f) {
			super.setVisible(false);
			return;
		}

		final float step = TIMER_DELAY_MS / (float) ANIMATION_DURATION_MS;

		fadeTimer = new Timer(TIMER_DELAY_MS, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				alpha -= step;
				if (alpha <= 0.0f) {
					alpha = 0.0f;
					fadeTimer.stop();
					SizeLabel.super.setVisible(false);  
				}
				repaint();
			}
		});

		fadeTimer.start();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		g2.setColor(AppColors.BACKGROUND);
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

		g2.setColor(AppColors.BORDER);
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

		g2.dispose();

		Graphics2D g2Text = (Graphics2D) g.create();
		g2Text.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		super.paintComponent(g2Text);
		g2Text.dispose();
	}
}