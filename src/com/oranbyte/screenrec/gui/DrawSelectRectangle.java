package com.oranbyte.screenrec.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class DrawSelectRectangle extends JPanel implements MouseListener, MouseMotionListener {

    private BufferedImage screenImage;
    Rectangle selectedRectangle = null;
    private Point dragOffset = null;
    private boolean isMoving = false;
    private boolean isCreated = false;

    public DrawSelectRectangle(BufferedImage screenImage) {
        this.screenImage = screenImage;
        addMouseListener(this);
        addMouseMotionListener(this);
        setOpaque(false);
        
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (selectedRectangle != null && screenImage != null) {
            try {
                BufferedImage cropped = screenImage.getSubimage(
                    selectedRectangle.x,
                    selectedRectangle.y,
                    selectedRectangle.width,
                    selectedRectangle.height
                );
                g2d.drawImage(cropped, selectedRectangle.x, selectedRectangle.y, null);
            } catch (Exception e) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(selectedRectangle.x, selectedRectangle.y,
                             selectedRectangle.width, selectedRectangle.height);
            }

            g2d.setColor(Color.ORANGE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(
                selectedRectangle.x,
                selectedRectangle.y,
                selectedRectangle.width,
                selectedRectangle.height,
                8, 8
            );
        }

        g2d.dispose();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();

        if (!isCreated) {
            selectedRectangle = new Rectangle(p.x, p.y, 0, 0);
            
        } else if (selectedRectangle.contains(p)) {
            dragOffset = new Point(p.x - selectedRectangle.x, p.y - selectedRectangle.y);
            isMoving = true;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();

        if (!isCreated && selectedRectangle != null) {
            int width = Math.abs(p.x - selectedRectangle.x);
            int height = Math.abs(p.y - selectedRectangle.y);
            selectedRectangle.setSize(width, height);
        } else if (isMoving && dragOffset != null && selectedRectangle != null) {
        	int newX = p.x - dragOffset.x;
            int newY = p.y - dragOffset.y;

            newX = Math.max(0, Math.min(newX, getWidth() - selectedRectangle.width));
            newY = Math.max(0, Math.min(newY, getHeight() - selectedRectangle.height));

            selectedRectangle.setLocation(newX, newY);
        }

        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!isCreated) {
            isCreated = true;
        }
        isMoving = false;
        dragOffset = null;
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {
        if (selectedRectangle != null && selectedRectangle.contains(e.getPoint())) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
        	if(!isCreated) {
        		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));        		
        	}else {
        		setCursor(Cursor.getDefaultCursor());
        	}
        }

    	
    }
}
