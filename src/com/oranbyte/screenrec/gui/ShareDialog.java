package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.components.NearbySharePanel;
import com.oranbyte.screenrec.gui.components.MainSharePanel;

public class ShareDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	public static final Color BG = AppColors.BACKGROUND;

	public static final String VIEW_MAIN = "MAIN_VIEW";
	public static final String VIEW_ALTERNATE = "ALTERNATE_VIEW";

	private final File file;
	private CardLayout cardLayout;
	private ScrollableCardPanel cardsPanel;
	private JScrollPane scrollPane;
	private NearbySharePanel nearbySharePanel;

	public ShareDialog(Frame owner, File file) {
		super(owner, "Share File", true);
		this.file = file;
		initUI();
	}

	private void initUI() {
		setSize(480, 520);
		setResizable(false);
		setLocationRelativeTo(getOwner());
		setLayout(new BorderLayout());
		getContentPane().setBackground(BG);
		setIconImage(Icons.SHARE.icon(32).getImage());

		cardLayout = new CardLayout();
		cardsPanel = new ScrollableCardPanel(cardLayout);
		cardsPanel.setBackground(BG);

		MainSharePanel mainPanel = new MainSharePanel(this, file);
		nearbySharePanel = new NearbySharePanel(this, file);

		cardsPanel.add(mainPanel, VIEW_MAIN);
		cardsPanel.add(nearbySharePanel, VIEW_ALTERNATE);

		scrollPane = new JScrollPane(cardsPanel);
		scrollPane.setBorder(null);
		scrollPane.getViewport().setBackground(BG);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getVerticalScrollBar().setBlockIncrement(64);

		add(scrollPane, BorderLayout.CENTER);
	}

	public void showMainView() {
		cardLayout.show(cardsPanel, VIEW_MAIN);
		resetScrollPosition();
	}

	public void showNearbySharePanel() {
		cardLayout.show(cardsPanel, VIEW_ALTERNATE);
		resetScrollPosition();
		nearbySharePanel.startDiscovery();
	}

	public void setViewPanel(JComponent component, String viewName) {
		cardsPanel.add(component, viewName);
		cardLayout.show(cardsPanel, viewName);
		resetScrollPosition();
	}

	private void resetScrollPosition() {
		if (scrollPane != null && scrollPane.getVerticalScrollBar() != null) {
			scrollPane.getVerticalScrollBar().setValue(0);
		}
		cardsPanel.revalidate();
		cardsPanel.repaint();
	}

	private static class ScrollableCardPanel extends JPanel implements Scrollable {
		private static final long serialVersionUID = 1L;

		public ScrollableCardPanel(CardLayout layout) {
			super(layout);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 64;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}
}