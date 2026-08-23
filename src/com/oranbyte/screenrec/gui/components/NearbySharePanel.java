package com.oranbyte.screenrec.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.ShareDialog;
import com.oranbyte.screenrec.share.FileShareManager;
import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendProvider;
import com.oranbyte.screenrec.util.FileUtil;

public class NearbySharePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Color CARD_BG = AppColors.CARD_BG;
	private static final Color PRIMARY_COLOR = AppColors.PRIMARY;
	private static final Color NEUTRAL_COLOR = AppColors.NEUTRAL_COLOR;
	private static final Color NEUTRAL_HOVER = AppColors.NEUTRAL_HOVER;
	private static final Color TEXT_PRIMARY = AppColors.TEXT;
	private static final Color TEXT_SECONDARY = AppColors.TEXT_SECONDARY;

	private static final int MAX_RELOAD_COUNT = 20;

	private final ShareDialog dialog;
	private final File file;
	private final FileShareManager manager;

	private DefaultListModel<ShareDevice> deviceListModel;
	private JList<ShareDevice> deviceList;
	private JLabel statusLabel;
	private ToolbarButton refreshBtn;

	private SwingWorker<Void, ShareDevice> discoveryWorker;
	private Timer rotationTimer;
	private double rotationAngle = 0;
	private boolean isScanning = false;
	private int scanCount = 0;

	public NearbySharePanel(ShareDialog dialog, File file) {
		this.dialog = dialog;
		this.file = file;

		setLayout(new BorderLayout(0, 15));
		setBorder(new EmptyBorder(20, 20, 20, 20));
		setBackground(ShareDialog.BG);

		FileShareProvider provider = new LocalSendProvider();
		this.manager = new FileShareManager(provider);

		initRotationTimer();
		initTopControlsAndHeader();
		initDeviceListUI();

		startDiscovery();
	}

	private void initRotationTimer() {
		rotationTimer = new Timer(30, e -> {
			rotationAngle += 10;
			if (rotationAngle >= 360) {
				rotationAngle = 0;
			}
			if (refreshBtn != null) {
				refreshBtn.repaint();
			}
		});
	}

	private void initTopControlsAndHeader() {
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setOpaque(false);
		topPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

		JButton backBtn = new JButton("← Back");
		backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		backBtn.setFont(AppConstant.APP_FONT.deriveFont(14f));
		backBtn.setFocusable(false);
		backBtn.addActionListener(e -> {
			stopDiscovery();
			dialog.showMainView();
		});
		topPanel.add(backBtn);

		topPanel.add(Box.createVerticalStrut(15));

		JPanel headerRow = new JPanel(new BorderLayout());
		headerRow.setOpaque(false);
		headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel header = new JLabel("Nearby Sharing");
		header.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 20f));
		header.setForeground(TEXT_PRIMARY);

		refreshBtn = new RotatingToolbarButton("", Icons.SCAN, 24);
		refreshBtn.setPadding(0, 0, 0, 0);
		refreshBtn.setHasBorder(false);
		refreshBtn.setToolTipText("Scan devices");
		refreshBtn.addActionListener(e -> {
			if (isScanning) {
				stopDiscovery();
			} else {
				startDiscovery();
			}
		});

		headerRow.add(header, BorderLayout.WEST);
		headerRow.add(refreshBtn, BorderLayout.EAST);
		topPanel.add(headerRow);

		topPanel.add(Box.createVerticalStrut(10));
		topPanel.add(createFileCard());

		topPanel.add(Box.createVerticalStrut(15));

		add(topPanel, BorderLayout.NORTH);
	}

	private RoundedPanel createFileCard() {
		RoundedPanel card = new RoundedPanel(14);
		card.setOpaque(false);
		card.setLayout(new BorderLayout(12, 0));
		card.setBackground(AppColors.CARD_BG);
		card.setBorderColor(AppColors.BORDER);
		card.setBorder(new EmptyBorder(12, 14, 12, 14));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);

		card.setMinimumSize(new Dimension(0, 72));
		card.setPreferredSize(new Dimension(320, 72));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

		JLabel icon = new JLabel(FileUtil.resolveFileIcon(file, 32));
		icon.setVerticalAlignment(SwingConstants.CENTER);

		JPanel info = new JPanel();
		info.setOpaque(false);
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

		String rawFileName = file != null ? file.getName() : "Unknown File";
		JLabel name = new JLabel(rawFileName) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getText() {
				String text = super.getText();
				if (text == null || getWidth() <= 0)
					return text;

				FontMetrics fm = getFontMetrics(getFont());
				int availableWidth = getWidth();
				if (fm.stringWidth(text) <= availableWidth) {
					return text;
				}

				String ellipsis = "...";
				int ellipsisWidth = fm.stringWidth(ellipsis);
				for (int i = text.length() - 1; i > 0; i--) {
					String truncated = text.substring(0, i);
					if (fm.stringWidth(truncated) + ellipsisWidth <= availableWidth) {
						return truncated + ellipsis;
					}
				}
				return text;
			}
		};
		name.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 13f));
		name.setForeground(AppColors.TEXT);
		name.setAlignmentX(Component.LEFT_ALIGNMENT);

		String fileSize = "";
		if (file != null) {
			double mb = file.length() / 1024d / 1024d;
			fileSize = mb >= 1 ? String.format("%.2f MB", mb) : String.format("%.0f KB", file.length() / 1024d);
		}

		JLabel size = new JLabel(fileSize);
		size.setFont(AppConstant.APP_FONT.deriveFont(11f));
		size.setForeground(TEXT_SECONDARY);
		size.setAlignmentX(Component.LEFT_ALIGNMENT);

		info.add(Box.createVerticalGlue());
		info.add(name);
		info.add(Box.createVerticalStrut(3));
		info.add(size);
		info.add(Box.createVerticalGlue());

		card.add(icon, BorderLayout.WEST);
		card.add(info, BorderLayout.CENTER);

		return card;
	}

	private void initDeviceListUI() {
		JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
		centerPanel.setOpaque(false);

		statusLabel = new JLabel("Scanning for nearby devices...");
		statusLabel.setFont(AppConstant.APP_FONT.deriveFont(Font.ITALIC, 12f));
		statusLabel.setForeground(TEXT_SECONDARY);
		centerPanel.add(statusLabel, BorderLayout.NORTH);

		deviceListModel = new DefaultListModel<>();
		deviceList = new JList<>(deviceListModel);
		deviceList.setCellRenderer(new DeviceListCellRenderer());
		deviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		deviceList.setBackground(ShareDialog.BG);
		deviceList.setFixedCellHeight(65);

		deviceList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = deviceList.locationToIndex(e.getPoint());
				if (index != -1) {
					ShareDevice device = deviceListModel.getElementAt(index);
					sendFileToDevice(device);
				}
			}
		});

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setOpaque(false);
		listWrapper.add(deviceList, BorderLayout.CENTER);

		centerPanel.add(listWrapper, BorderLayout.CENTER);
		add(centerPanel, BorderLayout.CENTER);
	}

	private void startDiscovery() {
		stopDiscovery();

		isScanning = true;
		scanCount = 0;
		rotationAngle = 0;
		rotationTimer.start();

		statusLabel.setText("Scanning for nearby devices...");
		deviceListModel.clear();

		discoveryWorker = new SwingWorker<>() {
			@Override
			protected Void doInBackground() throws Exception {
				while (!isCancelled() && scanCount < MAX_RELOAD_COUNT) {
					List<ShareDevice> devices = manager.getDevices();
					if (devices != null) {
						for (ShareDevice device : devices) {
							publish(device);
						}
					}
					scanCount++;
					Thread.sleep(2000);
				}
				return null;
			}

			@Override
			protected void process(List<ShareDevice> chunks) {
				for (ShareDevice device : chunks) {
					if (!deviceListModel.contains(device)) {
						deviceListModel.addElement(device);
						revalidate();
						repaint();
					}
				}
			}

			@Override
			protected void done() {
				if (!isCancelled() && scanCount >= MAX_RELOAD_COUNT) {
					SwingUtilities.invokeLater(() -> {
						stopDiscovery();
						statusLabel.setText("Scan complete (Max limit reached). Click refresh to rescan.");
					});
				}
			}
		};
		discoveryWorker.execute();

		if (refreshBtn != null) {
			refreshBtn.setToolTipText("Scanning...");
		}
	}

	private void stopDiscovery() {
		isScanning = false;
		if (rotationTimer != null && rotationTimer.isRunning()) {
			rotationTimer.stop();
		}
		if (discoveryWorker != null && !discoveryWorker.isDone()) {
			discoveryWorker.cancel(true);
		}
		if (refreshBtn != null) {
			refreshBtn.repaint();
			refreshBtn.setToolTipText("Scan devices");
		}
		statusLabel.setText("Scanning paused.");
	}

	private void sendFileToDevice(ShareDevice device) {
		if (file == null || !file.exists()) {
			JOptionPane.showMessageDialog(this, "Please select a valid file to send.", "File Missing",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this, "Send " + file.getName() + " to " + device.getName() + "?",
				"Confirm Transfer", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			statusLabel.setText("Sending file to " + device.getName() + "...");
		}
	}

	private JButton createStyledButton(String text, Color bg, Color fg) {
		JButton btn = new JButton(text) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(getBackground());
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		btn.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 12f));
		btn.setForeground(fg);
		btn.setBackground(bg);
		btn.setFocusable(false);
		btn.setContentAreaFilled(false);
		btn.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
		btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (bg.equals(NEUTRAL_COLOR)) {
					btn.setBackground(NEUTRAL_HOVER);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				btn.setBackground(bg);
			}
		});

		return btn;
	}

	private class RotatingToolbarButton extends ToolbarButton {
		private static final long serialVersionUID = 1L;

		public RotatingToolbarButton(String text, Icons icon, int size) {
			super(text, icon, size);
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (isScanning) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.rotate(Math.toRadians(rotationAngle), getWidth() / 2.0, getHeight() / 2.0);
				super.paintComponent(g2);
				g2.dispose();
			} else {
				super.paintComponent(g);
			}
		}
	}

	private class DeviceListCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			ShareDevice device = (ShareDevice) value;

			JPanel itemPanel = new JPanel(new BorderLayout(10, 0)) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(isSelected ? new Color(218, 232, 255) : CARD_BG);
					g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 10, 10);
					g2.dispose();
				}
			};
			itemPanel.setOpaque(false);
			itemPanel.setBorder(new EmptyBorder(8, 12, 8, 12));

			JPanel textContainer = new JPanel();
			textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.Y_AXIS));
			textContainer.setOpaque(false);

			JLabel nameLabel = new JLabel(device.getName());
			nameLabel.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 14f));
			nameLabel.setForeground(TEXT_PRIMARY);

			JLabel detailsLabel = new JLabel(device.getAddress() + ":" + device.getPort());
			detailsLabel.setFont(AppConstant.APP_FONT.deriveFont(11f));
			detailsLabel.setForeground(TEXT_SECONDARY);

			textContainer.add(nameLabel);
			textContainer.add(Box.createVerticalStrut(2));
			textContainer.add(detailsLabel);

			itemPanel.add(textContainer, BorderLayout.CENTER);

			JLabel sendLabel = new JLabel("Send ");
			sendLabel.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 12f));
			sendLabel.setForeground(PRIMARY_COLOR);
			itemPanel.add(sendLabel, BorderLayout.EAST);

			return itemPanel;
		}
	}
}