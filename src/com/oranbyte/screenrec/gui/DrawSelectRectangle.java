package com.oranbyte.screenrec.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.RecordingMode;
import com.oranbyte.screenrec.constants.RecordingState;
import com.oranbyte.screenrec.util.WindowFinder;

public class DrawSelectRectangle extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	private static final int HANDLE_SIZE = 12;
	private static final int MIN_SIZE = 10;

	private static final int NONE = -1;
	private static final int N = 0, S = 1, E = 2, W = 3, NE = 4, NW = 5, SE = 6, SW = 7;

	private BufferedImage screenImage;
	Rectangle selectedRectangle = null;
	private Point dragOffset = null;
	private boolean isMoving = false;
	private boolean isCreated = false;

	private CaptureMode captureMode;
	private RecordingMode recordingMode;

	// True while a recording is actively RECORDING or PAUSED.
	// While true, the selection rectangle is locked (no move/resize/reselect),
	// since the region being recorded must not change mid-capture.
	private boolean recordingActive = false;

	// Optional link back to the toolbar so this panel can report selection
	// progress (IDLE -> SELECTING -> READY). Wired up by whoever constructs
	// both (typically SelectionFrame) via setControlFrame(...).
	private ControlFrame controlFrame;

	private int activeHandle = NONE;
	private Point startPoint = null;
	private Point anchorPoint = null;
	private int fixedX, fixedY, fixedWidth, fixedHeight;
	private float dashPhase = 0f;
	private Rectangle hoverRectangle = null;
	private SelectionFrame selectionFrame;

	public DrawSelectRectangle(SelectionFrame selectionFrame, BufferedImage screenImage) {
		this.screenImage = screenImage;
		this.selectionFrame = selectionFrame;

		CaptureMode cm = selectionFrame.getCaptureMode();
		captureMode = cm == null ? CaptureMode.RECTANGLE : cm;

		RecordingMode rm = selectionFrame.getRecordingMode();
		recordingMode = rm == null ? RecordingMode.SCREENSHOT : rm;

		addMouseListener(this);
		addMouseMotionListener(this);
		setOpaque(false);

		Timer marchingAntsTimer = new Timer(50, e -> {
			dashPhase += 1f;

			if (dashPhase >= 10f) {
				dashPhase = 0f;
			}

			repaint();
		});
		marchingAntsTimer.start();
	}

	/**
	 * Links this panel to the toolbar so selection progress (IDLE / SELECTING /
	 * READY) can be reported as the user draws the capture region.
	 */
	public void setControlFrame(ControlFrame controlFrame) {
		this.controlFrame = controlFrame;
	}

	private void notifyState(RecordingState newState) {
		if (controlFrame != null) {
			controlFrame.setState(newState);
		}
	}

	public void setCaptureMode(CaptureMode mode) {
		if (recordingActive) {
			// Capture region is locked while a recording is in progress.
			return;
		}

		this.captureMode = mode;

		isMoving = false;
		dragOffset = null;
		activeHandle = NONE;
		anchorPoint = null;
		startPoint = null;
		hoverRectangle = null;

		if (mode == CaptureMode.ENTIRE_SCREEN) {
			selectedRectangle = computeFixedRectangle(mode);
			isCreated = true;
			notifyState(RecordingState.READY);
		} else {
			selectedRectangle = null;
			isCreated = false;
			notifyState(RecordingState.IDLE);
		}

		setCursor(mode == CaptureMode.RECTANGLE ? new Cursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
		repaint();
	}

	public CaptureMode getCaptureMode() {
		return captureMode;
	}

	/**
	 * Called by the toolbar (ControlFrame) whenever the recording process step
	 * changes, so this panel can enforce the right behavior for that step: -
	 * RECORDING / PAUSED (recordingActive = true): selection is frozen. - other
	 * steps (recordingActive = false): selection can be drawn/moved/resized.
	 */
	public void setRecordingActive(boolean recordingActive) {
		this.recordingActive = recordingActive;

		if (recordingActive) {
			// Cancel any in-flight drag/resize interaction so it doesn't
			// keep mutating the rectangle after recording has started.
			isMoving = false;
			dragOffset = null;
			activeHandle = NONE;
			anchorPoint = null;
			startPoint = null;
			setCursor(Cursor.getDefaultCursor());
		}

		repaint();
	}

	public boolean isRecordingActive() {
		return recordingActive;
	}

	public void setRecordingMode(RecordingMode mode) {
		this.recordingMode = mode;
	}

	public RecordingMode getRecordingMode() {
		return recordingMode;
	}

	private Rectangle computeFixedRectangle(CaptureMode mode) {
		if (mode == CaptureMode.ENTIRE_SCREEN) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			return new Rectangle(0, 0, screenSize.width - 1, screenSize.height - 1);
		}

		return null;
	}

	private Window getOverlayWindow() {
		if (selectionFrame instanceof Window) {
			return (Window) selectionFrame;
		}
		return SwingUtilities.getWindowAncestor(this);
	}

	private Rectangle clampToImageBounds(Rectangle rect) {
		if (screenImage == null || rect == null) {
			return rect;
		}

		int x = Math.max(0, rect.x);
		int y = Math.max(0, rect.y);
		int right = Math.min(rect.x + rect.width, screenImage.getWidth());
		int bottom = Math.min(rect.y + rect.height, screenImage.getHeight());
		int w = Math.max(0, right - x);
		int h = Math.max(0, bottom - y);

		return new Rectangle(x, y, w - 1, h - 1);
	}

	private void updateHoverRectangle(Point panelPoint) {
		Point panelLocation;
		try {
			panelLocation = getLocationOnScreen();
		} catch (Exception e) {
			return;
		}

		Point screenPoint = new Point(panelLocation.x + panelPoint.x, panelLocation.y + panelPoint.y);

		Rectangle windowBounds = WindowFinder.findWindowAt(screenPoint, getOverlayWindow());

		if (windowBounds == null) {
			hoverRectangle = null;
			return;
		}

		hoverRectangle = new Rectangle(windowBounds.x - panelLocation.x, windowBounds.y - panelLocation.y,
				windowBounds.width, windowBounds.height);
	}

	public void setFixedSelection(Rectangle rect) {
		if (rect == null) {
			return;
		}
		this.selectedRectangle = clampToImageBounds(new Rectangle(rect));
		this.isCreated = true;
		notifyState(RecordingState.READY);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setColor(new Color(0, 0, 0, 150));
		g2d.fillRect(0, 0, getWidth(), getHeight());

		if (selectedRectangle != null && screenImage != null) {
			Rectangle clipped = clampToImageBounds(selectedRectangle);
			try {
				BufferedImage cropped = screenImage.getSubimage(clipped.x, clipped.y, clipped.width, clipped.height);
				g2d.drawImage(cropped, selectedRectangle.x, selectedRectangle.y, null);

			} catch (Exception e) {
				g2d.setColor(Color.DARK_GRAY);
				g2d.fillRect(selectedRectangle.x, selectedRectangle.y, selectedRectangle.width,
						selectedRectangle.height);

			}
			g2d.setColor(AppColors.SELECTION_OUTLINE_COLOR);
			g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] { 6f, 4f },
					dashPhase));
			g2d.drawRoundRect(selectedRectangle.x, selectedRectangle.y, selectedRectangle.width,
					selectedRectangle.height, 8, 8);

			if (isCreated && !recordingActive) {
				drawHandles(g2d);
			}
		}

		if (captureMode == CaptureMode.WINDOW && !isCreated && hoverRectangle != null && screenImage != null) {
			Rectangle clipped = clampToImageBounds(hoverRectangle);
			try {
				BufferedImage cropped = screenImage.getSubimage(clipped.x, clipped.y, clipped.width, clipped.height);
				g2d.drawImage(cropped, hoverRectangle.x, hoverRectangle.y, null);
			} catch (Exception e) {
			}

			g2d.setColor(AppColors.SELECTION_OUTLINE_COLOR);
			g2d.setStroke(new BasicStroke(2f));
			g2d.drawRoundRect(hoverRectangle.x, hoverRectangle.y, hoverRectangle.width, hoverRectangle.height, 8, 8);
		}
		g2d.dispose();
	}

	private void drawHandles(Graphics2D g2d) {
		g2d.setColor(AppColors.SELECTION_RESIZER_COLOR);
		for (Rectangle handle : getHandleRects().values()) {
			g2d.fillRect(handle.x, handle.y, handle.width, handle.height);
		}
	}

	private Map<Integer, Rectangle> getHandleRects() {
		Map<Integer, Rectangle> handles = new HashMap<>();
		if (selectedRectangle == null) {
			return handles;
		}
		int x = selectedRectangle.x;
		int y = selectedRectangle.y;
		int w = selectedRectangle.width;
		int h = selectedRectangle.height;
		int hs = HANDLE_SIZE;
		int half = hs / 2;

		handles.put(NW, new Rectangle(x - half, y - half, hs, hs));
		handles.put(N, new Rectangle(x + w / 2 - half, y - half, hs, hs));
		handles.put(NE, new Rectangle(x + w - half, y - half, hs, hs));
		handles.put(W, new Rectangle(x - half, y + h / 2 - half, hs, hs));
		handles.put(E, new Rectangle(x + w - half, y + h / 2 - half, hs, hs));
		handles.put(SW, new Rectangle(x - half, y + h - half, hs, hs));
		handles.put(S, new Rectangle(x + w / 2 - half, y + h - half, hs, hs));
		handles.put(SE, new Rectangle(x + w - half, y + h - half, hs, hs));

		return handles;
	}

	private int getHandleAt(Point p) {
		if (selectedRectangle == null) {
			return NONE;
		}
		for (Map.Entry<Integer, Rectangle> entry : getHandleRects().entrySet()) {
			if (entry.getValue().contains(p)) {
				return entry.getKey();
			}
		}
		return NONE;
	}

	private Cursor cursorForHandle(int handle) {
		switch (handle) {
		case N:
		case S:
			return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
		case E:
		case W:
			return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
		case NE:
			return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
		case NW:
			return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
		case SE:
			return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
		case SW:
			return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
		default:
			return Cursor.getDefaultCursor();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {

		// While a recording is RECORDING or PAUSED, the capture region is frozen:
		// ignore all press interactions so the user can't accidentally
		// move/resize/reselect the area being recorded.
		if (recordingActive) {
			return;
		}

		Point p = e.getPoint();

		if (!isCreated) {
			if (captureMode == CaptureMode.WINDOW) {
				if (hoverRectangle != null) {
					selectedRectangle = clampToImageBounds(new Rectangle(hoverRectangle));
					isCreated = true;
					hoverRectangle = null;
					notifyState(RecordingState.READY);
					repaint();
				}
				return;
			}

			if (captureMode == CaptureMode.ENTIRE_SCREEN) {
				selectedRectangle = computeFixedRectangle(captureMode);
				isCreated = true;
				notifyState(RecordingState.READY);
				repaint();
				return;
			}

			startPoint = p;
			selectedRectangle = new Rectangle(p.x, p.y, 0, 0);
			notifyState(RecordingState.SELECTING);
			return;
		}

		int handle = getHandleAt(p);
		if (handle != NONE) {
			activeHandle = handle;

			fixedX = selectedRectangle.x;
			fixedY = selectedRectangle.y;
			fixedWidth = selectedRectangle.width;
			fixedHeight = selectedRectangle.height;

			int anchorX, anchorY;

			switch (handle) {
			case NW:
				anchorX = fixedX + fixedWidth;
				anchorY = fixedY + fixedHeight;
				break;
			case NE:
				anchorX = fixedX;
				anchorY = fixedY + fixedHeight;
				break;
			case SW:
				anchorX = fixedX + fixedWidth;
				anchorY = fixedY;
				break;
			case SE:
				anchorX = fixedX;
				anchorY = fixedY;
				break;
			case N:
				anchorX = fixedX;
				anchorY = fixedY + fixedHeight;
				break;
			case S:
				anchorX = fixedX;
				anchorY = fixedY;
				break;
			case W:
				anchorX = fixedX + fixedWidth;
				anchorY = fixedY;
				break;
			case E:
				anchorX = fixedX;
				anchorY = fixedY;
				break;
			default:
				anchorX = fixedX;
				anchorY = fixedY;
			}

			anchorPoint = new Point(anchorX, anchorY);
			notifyState(RecordingState.SELECTING);
		} else if (selectedRectangle.contains(p)) {
			dragOffset = new Point(p.x - selectedRectangle.x, p.y - selectedRectangle.y);
			isMoving = true;
			notifyState(RecordingState.SELECTING);
		} else if (captureMode == CaptureMode.RECTANGLE) {
			isCreated = false;
			isMoving = false;
			dragOffset = null;
			activeHandle = NONE;
			anchorPoint = null;
			startPoint = p;
			selectedRectangle = new Rectangle(p.x, p.y, 0, 0);
			notifyState(RecordingState.SELECTING);
		}
//
//		if (recordingMode == RecordingMode.SCREENSHOT) {
//			// Screenshots are a single fire-and-forget capture with no
//			// ongoing "process step" beyond SELECTING/READY: the rectangle
//			// stays freely editable right up until the user triggers the
//			// actual capture action elsewhere in the UI.
//		} else if (recordingMode == RecordingMode.VIDEO) {
//			// Reaching this point means recordingActive is false (guarded
//			// above), i.e. we're still adjusting the region pre-recording,
//			// so editing the selection here is expected and allowed.
//
//		}

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (recordingActive) {
			return;
		}

		Point p = e.getPoint();

		if (!isCreated && selectedRectangle != null && startPoint != null) {

			int px = Math.max(0, Math.min(p.x, getWidth()));
			int py = Math.max(0, Math.min(p.y, getHeight()));

			int x = Math.min(startPoint.x, px);
			int y = Math.min(startPoint.y, py);
			int width = Math.abs(px - startPoint.x);
			int height = Math.abs(py - startPoint.y);

			selectedRectangle.setBounds(x, y, width, height);
		} else if (activeHandle != NONE && anchorPoint != null) {
			resizeSelection(p);
		} else if (isMoving && dragOffset != null && selectedRectangle != null) {
			int newX = p.x - dragOffset.x;
			int newY = p.y - dragOffset.y;
			newX = Math.max(0, Math.min(newX, getWidth() - selectedRectangle.width));
			newY = Math.max(0, Math.min(newY, getHeight() - selectedRectangle.height));
			selectedRectangle.setLocation(newX, newY);
		}
		repaint();
	}

	private void resizeSelection(Point p) {
		int px = Math.max(0, Math.min(p.x, getWidth()));
		int py = Math.max(0, Math.min(p.y, getHeight()));

		int newX = selectedRectangle.x;
		int newY = selectedRectangle.y;
		int newW = selectedRectangle.width;
		int newH = selectedRectangle.height;

		boolean affectsX = activeHandle == N || activeHandle == S ? false : true;
		boolean affectsY = activeHandle == E || activeHandle == W ? false : true;

		if (affectsX) {
			newX = Math.min(anchorPoint.x, px);
			newW = Math.abs(px - anchorPoint.x);
		}
		if (affectsY) {
			newY = Math.min(anchorPoint.y, py);
			newH = Math.abs(py - anchorPoint.y);
		}

		if (newW < MIN_SIZE) {
			newW = MIN_SIZE;
			if (affectsX) {
				newX = (px < anchorPoint.x) ? anchorPoint.x - MIN_SIZE : anchorPoint.x;
			}
		}
		if (newH < MIN_SIZE) {
			newH = MIN_SIZE;
			if (affectsY) {
				newY = (py < anchorPoint.y) ? anchorPoint.y - MIN_SIZE : anchorPoint.y;
			}
		}

		newX = Math.max(0, newX);
		newY = Math.max(0, newY);
		if (newX + newW > getWidth()) {
			newW = getWidth() - newX;
		}
		if (newY + newH > getHeight()) {
			newH = getHeight() - newY;
		}

		selectedRectangle.setBounds(newX, newY, newW, newH);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (recordingActive) {
			return;
		}

		boolean wasFreshSelection = !isCreated;

		if (!isCreated) {
			isCreated = true;
		}
		isMoving = false;
		dragOffset = null;
		activeHandle = NONE;
		anchorPoint = null;
		startPoint = null;

		// Selection finished (whether it's a brand-new drag, a resize, or a
		// move) -> the region is ready to record/capture.
		if (selectedRectangle != null && selectedRectangle.width >= MIN_SIZE && selectedRectangle.height >= MIN_SIZE) {
			notifyState(RecordingState.READY);
		} else if (wasFreshSelection) {
			// Released without dragging out a usable rectangle: back to IDLE.
			isCreated = false;
			notifyState(RecordingState.IDLE);
		}

		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (recordingActive) {
			setCursor(Cursor.getDefaultCursor());
			return;
		}

		if (captureMode == CaptureMode.WINDOW && !isCreated) {
			updateHoverRectangle(e.getPoint());
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			repaint();
			return;
		}

		if (selectedRectangle == null) {
			return;
		}

		if (isCreated) {
			int handle = getHandleAt(e.getPoint());
			if (handle != NONE) {
				setCursor(cursorForHandle(handle));
				return;
			}

			if (selectedRectangle.contains(e.getPoint())) {
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			} else {
				setCursor(Cursor.getDefaultCursor());
			}
			return;
		}

		if (selectedRectangle.contains(e.getPoint())) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		} else {
			setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		}
	}

	public Rectangle getSelectedRectangle() {
		return selectedRectangle == null ? null : new Rectangle(selectedRectangle);
	}

	public void setScreenImage(BufferedImage image) {
		this.screenImage = image;
	}
}