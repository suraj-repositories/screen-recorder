package com.oranbyte.screenrec.gui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import com.oranbyte.screenrec.constants.AppColors;

class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int radius;
        private Color borderColor = AppColors.BORDER;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        void setBorderColor(Color color) { this.borderColor = color; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }