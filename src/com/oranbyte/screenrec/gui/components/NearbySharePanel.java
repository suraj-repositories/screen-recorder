package com.oranbyte.screenrec.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.ShareDialog;
import com.oranbyte.screenrec.util.FileUtil;
import com.oranbyte.screenrec.util.NotificationUtil;
import com.oranbyte.screenrec.share.FileShareManager;
import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.TransferListener;
import com.oranbyte.screenrec.share.localsend.LocalSendProvider;

public class NearbySharePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Color CARD_BG = AppColors.CARD_BG;
	private static final Color TEXT_PRIMARY = AppColors.TEXT;
	private static final Color TEXT_SECONDARY = AppColors.TEXT_SECONDARY;

	private static final int MAX_RELOAD_COUNT = AppConstant.NEARBY_SCAN_TIMEOUT;
	private static final int CONNECTION_TIMEOUT_MS = 15000;

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

	private final Map<String, TransferState> transferStates = new HashMap<>();
	private final Map<String, Timer> connectionTimeoutTimers = new HashMap<>();

	private static class TransferState {
		boolean isConnecting = false;
		boolean isSending = false;
		boolean isComplete = false;
		int progressPercent = 0;
		String statusText = "";
	}

	public NearbySharePanel(ShareDialog dialog, File file) {
		this.dialog = dialog;
		this.file = file;

		setLayout(new BorderLayout(0, 15));
		setBorder(new EmptyBorder(20, 20, 20, 20));
		setBackground(dialog.getShareDialogBackground());

		FileShareProvider provider = new LocalSendProvider();
		this.manager = new FileShareManager(provider);

		initRotationTimer();
		initTopControlsAndHeader();
		initDeviceListUI();
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
		refreshBtn.addActionListener(e -> startDiscovery());

		headerRow.add(header, BorderLayout.WEST);
		headerRow.add(refreshBtn, BorderLayout.EAST);
		topPanel.add(headerRow);

		topPanel.add(Box.createVerticalStrut(10));
		topPanel.add(createFileCard());

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
		deviceList.setBackground(dialog.getShareDialogBackground());
		deviceList.setFocusable(false);
		deviceList.setFixedCellHeight(76);

		deviceList.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				deviceList.clearSelection();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				deviceList.clearSelection();
				int index = deviceList.locationToIndex(e.getPoint());
				if (index != -1) {
					Rectangle bounds = deviceList.getCellBounds(index, index);
					if (bounds.contains(e.getPoint()) && isOverSendButton(e, bounds)) {
						ShareDevice selectedDevice = deviceListModel.getElementAt(index);
						String deviceId = getDeviceId(selectedDevice);
						TransferState state = transferStates.get(deviceId);
						if (state == null || (!state.isConnecting && !state.isSending && !state.isComplete)) {
							sendFileToDevice(selectedDevice);
						}
					}
				}
			}
		});

		deviceList.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int index = deviceList.locationToIndex(e.getPoint());
				if (index != -1) {
					Rectangle bounds = deviceList.getCellBounds(index, index);
					if (bounds.contains(e.getPoint()) && isOverSendButton(e, bounds)) {
						ShareDevice selectedDevice = deviceListModel.getElementAt(index);
						String deviceId = getDeviceId(selectedDevice);
						TransferState state = transferStates.get(deviceId);
						if (state == null || (!state.isConnecting && !state.isSending && !state.isComplete)) {
							deviceList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
							return;
						}
					}
				}
				deviceList.setCursor(Cursor.getDefaultCursor());
			}
		});

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setOpaque(false);
		listWrapper.add(deviceList, BorderLayout.CENTER);

		centerPanel.add(listWrapper, BorderLayout.CENTER);
		add(centerPanel, BorderLayout.CENTER);
	}

	private boolean isOverSendButton(MouseEvent e, Rectangle bounds) {
		int buttonSize = 36;
		int paddingRight = 12;
		Rectangle buttonBounds = new Rectangle(bounds.x + bounds.width - buttonSize - paddingRight,
				bounds.y + (bounds.height - buttonSize) / 2, buttonSize, buttonSize);
		return buttonBounds.contains(e.getPoint());
	}

	private String getDeviceId(ShareDevice device) {
		if (device == null)
			return "";
		return device.getAddress() + ":" + device.getPort();
	}

	public void reset() {
		stopDiscovery();
		deviceListModel.clear();
		transferStates.clear();
		clearAllTimeouts();
	}

	public void startDiscovery() {
		stopDiscovery();

		if (!manager.isStarted()) {
			manager.start();
		}

		isScanning = true;
		scanCount = 0;
		rotationAngle = 0;
		rotationTimer.start();

		statusLabel.setText("Scanning for nearby devices...");
		deviceListModel.clear();
		transferStates.clear();
		clearAllTimeouts();

		discoveryWorker = new SwingWorker<>() {
			@Override
			protected Void doInBackground() throws Exception {
				while (!isCancelled() && scanCount < MAX_RELOAD_COUNT) {
					List<ShareDevice> devices = manager.getDevices();
					if (devices != null && !devices.isEmpty()) {
						for (ShareDevice device : devices) {
							publish(device);
						}
					}
					scanCount++;
					Thread.sleep(1500);
				}
				return null;
			}

			@Override
			protected void process(List<ShareDevice> chunks) {
				for (ShareDevice device : chunks) {
					if (!deviceListModel.contains(device)) {
						deviceListModel.addElement(device);
					}
				}
				updateStatusAfterScan();
			}

			@Override
			protected void done() {
				if (!isCancelled()) {
					SwingUtilities.invokeLater(() -> {
						stopScanningState();
						updateStatusAfterScan();
					});
				}
			}
		};
		discoveryWorker.execute();

		if (refreshBtn != null) {
			refreshBtn.setToolTipText("Scanning...");
		}
	}

	private void updateStatusAfterScan() {
		int count = deviceListModel.getSize();
		if (count == 0) {
			if (isScanning) {
				statusLabel.setText("Scanning for nearby devices...");
			} else {
				statusLabel.setText("No devices found.");
			}
		} else {
			statusLabel.setText("Found " + count + " device" + (count > 1 ? "s" : "") + ".");
		}
	}

	private void stopScanningState() {
		isScanning = false;
		if (rotationTimer != null && rotationTimer.isRunning()) {
			rotationTimer.stop();
		}
		if (refreshBtn != null) {
			refreshBtn.repaint();
			refreshBtn.setToolTipText("Scan devices");
		}
	}

	private void stopDiscovery() {
		stopScanningState();
		if (discoveryWorker != null && !discoveryWorker.isDone()) {
			discoveryWorker.cancel(true);
		}
		if (manager != null && manager.isStarted()) {
			manager.stop();
		}
	}

	private void clearAllTimeouts() {
		for (Timer timer : connectionTimeoutTimers.values()) {
			if (timer.isRunning()) {
				timer.stop();
			}
		}
		connectionTimeoutTimers.clear();
	}

	private void cancelTimeout(String deviceId) {
		Timer timer = connectionTimeoutTimers.remove(deviceId);
		if (timer != null && timer.isRunning()) {
			timer.stop();
		}
	}

	private void sendFileToDevice(ShareDevice device) {
		if (file == null || !file.exists()) {
			NotificationUtil.info("File Missing", "Please select a valid file to send.");
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this, "Send " + file.getName() + " to " + device.getName() + "?",
				"Confirm Transfer", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			String deviceId = getDeviceId(device);
			TransferState state = transferStates.computeIfAbsent(deviceId, k -> new TransferState());
			state.isConnecting = true;
			state.isSending = false;
			state.isComplete = false;
			state.progressPercent = 0;
			state.statusText = "Connecting...";
			deviceList.repaint();
 
			cancelTimeout(deviceId);
			Timer timeoutTimer = new Timer(CONNECTION_TIMEOUT_MS, e -> {
				if (state.isConnecting) {
					state.isConnecting = false;
					state.statusText = "Timeout";
					deviceList.repaint();
					NotificationUtil.error("Connection Timeout", "Could not connect to " + device.getName());

					// Clear status text after 2 seconds
					Timer resetStateTimer = new Timer(2000, ev -> {
						state.statusText = "";
						deviceList.repaint();
					});
					resetStateTimer.setRepeats(false);
					resetStateTimer.start();
				}
			});
			timeoutTimer.setRepeats(false);
			connectionTimeoutTimers.put(deviceId, timeoutTimer);
			timeoutTimer.start();

			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					manager.send(file, device, new TransferListener() {
						@Override
						public void onStarted(long totalBytes) {
							SwingUtilities.invokeLater(() -> {
								cancelTimeout(deviceId);
								state.isConnecting = false;
								state.isSending = true;
								state.statusText = "Sending...";
								deviceList.repaint();
							});
						}

						@Override
						public void onProgress(long transferred, long total) {
							int percent = (int) (((double) transferred / total) * 100);
							SwingUtilities.invokeLater(() -> {
								cancelTimeout(deviceId);
								state.isConnecting = false;
								state.isSending = true;
								state.progressPercent = percent;
								state.statusText = percent + "%";
								deviceList.repaint();
							});
						}

						@Override
						public void onCompleted() {
							SwingUtilities.invokeLater(() -> {
								cancelTimeout(deviceId);
								state.isConnecting = false;
								state.isSending = false;
								state.isComplete = true;
								state.statusText = "Sent!";
								deviceList.repaint();

								Timer revertTimer = new Timer(2000, e -> {
									state.isComplete = false;
									state.statusText = "";
									deviceList.repaint();
								});
								revertTimer.setRepeats(false);
								revertTimer.start();

								NotificationUtil.success("File Sent", "Your file was sent successfully.");
							});
						}

						@Override
						public void onFailed(Exception e) {
							SwingUtilities.invokeLater(() -> {
								cancelTimeout(deviceId);
								state.isConnecting = false;
								state.isSending = false;
								state.isComplete = false;
								state.statusText = "Failed";
								deviceList.repaint();
								NotificationUtil.error("Failed to send file", e.getMessage());
							});
						}

						@Override
						public void onCancelled() {
							SwingUtilities.invokeLater(() -> {
								cancelTimeout(deviceId);
								state.isConnecting = false;
								state.isSending = false;
								state.isComplete = false;
								state.statusText = "Cancelled";
								deviceList.repaint();
							});
						}
					});
					return null;
				}
			}.execute();
		}
	}

	private ImageIcon resolveDeviceIcon(ShareDevice device) {
		if (device == null || device.getDeviceType() == null) {
			return Icons.DESKTOP != null ? Icons.DESKTOP.icon(28) : null;
		}

		String type = device.getDeviceType().toLowerCase();
		if (type.contains("mobile") || type.contains("phone") || type.contains("android") || type.contains("iphone")) {
			return Icons.PHONE != null ? Icons.PHONE.icon(28) : null;
		} else if (type.contains("tablet") || type.contains("ipad")) {
			return Icons.TABLET != null ? Icons.TABLET.icon(28) : null;
		} else {
			return Icons.DESKTOP != null ? Icons.DESKTOP.icon(28) : null;
		}
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

		private final Map<String, Timer> activeTimers = new HashMap<>();
		private final Map<String, LoadingIcon> loadingIcons = new HashMap<>();

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			ShareDevice device = (ShareDevice) value;
			String deviceId = getDeviceId(device);
			TransferState state = transferStates.get(deviceId);

			JPanel itemPanel = new JPanel(new BorderLayout(12, 0)) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void paintComponent(Graphics g) {
					Graphics2D g2 = (Graphics2D) g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					int arc = 12;
					int x = 1;
					int y = 3;
					int width = getWidth() - 3;
					int height = getHeight() - 7;

					g2.setColor(CARD_BG);
					g2.fillRoundRect(x, y, width, height, arc, arc);

					g2.setColor(AppColors.BORDER);
					g2.drawRoundRect(x, y, width, height, arc, arc);

					g2.dispose();
				}
			};

			itemPanel.setOpaque(false);
			itemPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2),
					BorderFactory.createEmptyBorder(6, 12, 6, 12)));

			JLabel deviceIconLabel = new JLabel(resolveDeviceIcon(device));
			deviceIconLabel.setVerticalAlignment(SwingConstants.CENTER);
			itemPanel.add(deviceIconLabel, BorderLayout.WEST);

			JPanel textContainer = new JPanel();
			textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.Y_AXIS));
			textContainer.setOpaque(false);

			JLabel nameLabel = new JLabel(device != null ? device.getName() : "Unknown");
			nameLabel.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 14f));
			nameLabel.setForeground(TEXT_PRIMARY);
			nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

			String baseAddress = (device != null) ? (device.getAddress() + ":" + device.getPort()) : "";
			String statusText = (state != null && state.statusText != null && !state.statusText.isEmpty())
					? " • " + state.statusText
					: "";

			JLabel detailsLabel = new JLabel(baseAddress + statusText);
			detailsLabel.setFont(AppConstant.APP_FONT.deriveFont(11f));
			detailsLabel.setForeground(TEXT_SECONDARY);
			detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

			textContainer.add(Box.createVerticalGlue());
			textContainer.add(nameLabel);
			textContainer.add(Box.createVerticalStrut(3));
			textContainer.add(detailsLabel);
			textContainer.add(Box.createVerticalGlue());

			itemPanel.add(textContainer, BorderLayout.CENTER);

			ToolbarButton sendButton;

			if (state != null && state.isConnecting) {
				LoadingIcon loadingIcon = loadingIcons.computeIfAbsent(deviceId,
						k -> new LoadingIcon(22, 2, Color.GRAY));
				sendButton = new ToolbarButton(loadingIcon);
				sendButton.setEnabled(false);

				if (deviceId != null && !activeTimers.containsKey(deviceId)) {
					Timer timer = new Timer(50, e -> {
						loadingIcon.rotate();
						list.repaint();
					});
					activeTimers.put(deviceId, timer);
					timer.start();
				}

			} else {
				if (deviceId != null) {
					loadingIcons.remove(deviceId);
					Timer timer = activeTimers.remove(deviceId);
					if (timer != null && timer.isRunning()) {
						timer.stop();
					}
				}

				if (state != null && state.isSending) {
					sendButton = new ToolbarButton(state.progressPercent + "%");
					sendButton.setEnabled(false);
				} else if (state != null && state.isComplete) {
					sendButton = new ToolbarButton(Icons.CHECK_GREEN, 22);
					sendButton.setEnabled(false);
					sendButton.setDisabledIcon(sendButton.getIcon());
				} else {
					sendButton = new ToolbarButton(Icons.UPLOAD, 22);
					sendButton.setEnabled(true);
				}
			}

			sendButton.setToolTipText("Send to " + (device != null ? device.getName() : "device"));
			sendButton.setFocusable(false);
			sendButton.setFont(AppConstant.APP_FONT.deriveFont(11f));
			sendButton.setPadding(0, 0, 0, 0);
			sendButton.setHoverBackgroundColor(AppColors.GRAY_100);
			sendButton.setHorizontalAlignment(SwingConstants.CENTER);
			sendButton.setVerticalAlignment(SwingConstants.CENTER);

			sendButton.setHorizontalTextPosition(SwingConstants.CENTER);
			sendButton.setVerticalTextPosition(SwingConstants.CENTER);

			int btnSize = 32;
			sendButton.setPreferredSize(new Dimension(btnSize, btnSize));
			sendButton.setMaximumSize(new Dimension(btnSize, btnSize));
			sendButton.setMinimumSize(new Dimension(btnSize, btnSize));

			JPanel actionWrapper = new JPanel(new GridBagLayout());
			actionWrapper.setOpaque(false);
			actionWrapper.add(sendButton);

			itemPanel.add(actionWrapper, BorderLayout.EAST);

			return itemPanel;
		}
	}

}