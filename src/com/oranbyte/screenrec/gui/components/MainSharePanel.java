package com.oranbyte.screenrec.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.gui.ShareDialog;
import com.oranbyte.screenrec.util.FileUtil;

public class MainSharePanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;
	private static final Color CARD = AppColors.WHITE;
	private static final Color BORDER = new Color(228, 230, 236);
	private static final Color TEXT = new Color(30, 30, 34);
	private static final Color MUTED = AppColors.TEXT_MUTED;

	private final ShareDialog dialog;
	private final File file;
	private ToolbarButton copyBtn;

	public MainSharePanel(ShareDialog dialog, File file) {
		this.dialog = dialog;
		this.file = file;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(22, 22, 22, 22));
		setBackground(dialog.getShareDialogBackground());

		initUI();
	}

	private void initUI() {
		addFileCard();
		add(Box.createVerticalStrut(18));

		addShareButtons();
		add(Box.createVerticalStrut(18));

		addNearbyShareButton();
		add(Box.createVerticalStrut(18));
	}

	private void addFileCard() {
		RoundedPanel card = new RoundedPanel(14);
		card.setLayout(new BorderLayout(14, 0));
		card.setBackground(CARD);
		card.setBorderColor(BORDER);
		card.setBorder(new EmptyBorder(14, 16, 14, 16));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
		card.setPreferredSize(new Dimension(200, 72));

		JLabel icon = new JLabel(FileUtil.resolveFileIcon(file, 32));
		icon.setBorder(new EmptyBorder(0, 0, 0, 6));
		icon.setVerticalAlignment(SwingConstants.CENTER);

		JPanel info = new JPanel();
		info.setOpaque(false);
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

		String fileName = file != null ? file.getName() : "Unknown File";
		if (fileName.length() > 36) {
			fileName = fileName.substring(0, 33) + "...";
		}

		JLabel name = new JLabel(fileName);
		name.setFont(AppConstant.APP_FONT.deriveFont(14f));
		name.setForeground(TEXT);
		name.setAlignmentX(Component.LEFT_ALIGNMENT);

		String fileSize = "";
		if (file != null) {
			double mb = file.length() / 1024d / 1024d;
			fileSize = mb >= 1 ? String.format("%.2f MB", mb) : String.format("%.0f KB", file.length() / 1024d);
		}

		JLabel size = new JLabel(fileSize);
		size.setFont(AppConstant.APP_FONT.deriveFont(12f));
		size.setForeground(MUTED);
		size.setAlignmentX(Component.LEFT_ALIGNMENT);

		info.add(Box.createVerticalGlue());
		info.add(name);
		info.add(Box.createVerticalStrut(4));
		info.add(size);
		info.add(Box.createVerticalGlue());

		copyBtn = new ToolbarButton(Icons.COPY, 24);
		copyBtn.setBorder(null);
		copyBtn.setSm();
		copyBtn.setBorderRadius(10);
		copyBtn.setPreferredSize(new Dimension(38, 38));
		copyBtn.setMinimumSize(new Dimension(38, 38));
		copyBtn.setMaximumSize(new Dimension(38, 38));
		copyBtn.setFocusable(false);
		copyBtn.setToolTipText("Copy file to clipboard");
		copyBtn.addActionListener(e -> handleCopyFile(copyBtn));

		JPanel actionPanel = new JPanel(new GridBagLayout());
		actionPanel.setOpaque(false);
		actionPanel.add(copyBtn);

		card.add(icon, BorderLayout.WEST);
		card.add(info, BorderLayout.CENTER);
		card.add(actionPanel, BorderLayout.EAST);

		add(card);
	}

	private void addShareButtons() {
		JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
		grid.setOpaque(false);
		grid.setAlignmentX(Component.LEFT_ALIGNMENT);
		grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

		grid.add(createShareButton("WhatsApp", Icons.WHATSAPP, e -> shareToPlatform("whatsapp")));
		grid.add(createShareButton("Telegram", Icons.TELEGRAM, e -> shareToPlatform("telegram")));
		grid.add(createShareButton("Email", Icons.EMAIL, e -> shareToPlatform("email")));
		grid.add(createShareButton("Twitter / X", Icons.TWITTER, e -> shareToPlatform("twitter")));

		add(grid);
	}

	private void addNearbyShareButton() {
		JPanel sharePanel = new JPanel();
		sharePanel.setLayout(new BoxLayout(sharePanel, BoxLayout.Y_AXIS));
		sharePanel.setOpaque(false);
		sharePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel titleLabel = new JLabel("Share Using -");
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		titleLabel.setFont(AppConstant.APP_FONT.deriveFont(16f));

		sharePanel.add(titleLabel);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
		buttonPanel.setOpaque(false);
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		int buttonHeight = 130;

		ToolbarButton nearbySharing = createCardButton("<html>Nearby<br>Sharing</html>", Icons.SHARE, "Share locally");
		nearbySharing.addActionListener(e -> dialog.showNearbySharePanel());

		ToolbarButton fileManager = createCardButton("File Manager", Icons.FOLDER, "Start file manager");
		fileManager.addActionListener(this::handleDesktopShare);

		ToolbarButton mediaViewer = createCardButton("Media Viewer",
				FileUtil.isImage(file) ? Icons.IMAGE : Icons.PLAY_VIDEO_CIRCLE, "Open media viewer");
		mediaViewer.addActionListener(e -> {
			try {
				if (file != null)
					Desktop.getDesktop().open(file);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		buttonPanel.add(nearbySharing);
		buttonPanel.add(fileManager);
		buttonPanel.add(mediaViewer);

		buttonPanel.setPreferredSize(new Dimension(0, buttonHeight));
		buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonHeight));

		sharePanel.add(buttonPanel);
		add(sharePanel);
	}

	private ToolbarButton createShareButton(String text, Icons icon, ActionListener listener) {
		ToolbarButton button = new ToolbarButton(text, icon, 24);
		button.setFocusable(false);
		button.setBackgroundColor(AppColors.WHITE);
		button.addActionListener(listener);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setVerticalAlignment(SwingConstants.CENTER);
		button.setHorizontalTextPosition(SwingConstants.RIGHT);
		button.setVerticalTextPosition(SwingConstants.CENTER);
		button.setIconTextGap(8);
		return button;
	}

	private ToolbarButton createCardButton(String label, Icons icon, String tip) {
		ToolbarButton btn = new ToolbarButton(label, icon, 50);
		btn.setHasBorder(false);
		btn.setBorderRadius(5);
		btn.setHorizontalAlignment(SwingConstants.CENTER);
		btn.setVerticalAlignment(SwingConstants.CENTER);
		btn.setHorizontalTextPosition(SwingConstants.CENTER);
		btn.setVerticalTextPosition(SwingConstants.BOTTOM);
		btn.setToolTipText(tip);
		btn.setBackground(null);
		btn.setBackgroundColor(null);
		btn.setBorderRadius(10);
		btn.setHoverBackgroundColor(AppColors.GRAY_200);

		btn.setMaximumSize(new Dimension(btn.getWidth(), btn.getWidth()));

		return btn;
	}

	private void shareToPlatform(String platform) {
		if (file == null)
			return;
		handleCopyFile(copyBtn);

		try {
			String label = "Check this file: " + file.getName();
			String message = label + "\n\n(File is also on your clipboard – just paste it)";
			String path = file.getAbsolutePath();

			String url = switch (platform) {
			case "whatsapp" -> "https://web.whatsapp.com/send?text=" + encodeForUrl(message);
			case "telegram" ->
				"https://t.me/share/url?url=" + encodeForUrl(file.toURI().toString()) + "&text=" + encodeForUrl(label);
			case "twitter" -> "https://twitter.com/intent/tweet?text=" + encodeForUrl(message);
			case "email" -> {
				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
					try {
						Desktop.getDesktop().mail(new URI("mailto:?subject=" + encodeForUrl(label) + "&body="
								+ encodeForUrl(message + "\n\nPath: " + path)));
						yield null;
					} catch (Exception ignored) {
					}
				}
				yield "mailto:?subject=" + encodeForUrl(label) + "&body=" + encodeForUrl(message + "\n\nPath: " + path);
			}
			default -> null;
			};

			if (url != null) {
				Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleCopyFile(JButton button) {
		if (file == null)
			return;
		try {
			Transferable transferable = new Transferable() {
				@Override
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[] { DataFlavor.javaFileListFlavor };
				}

				@Override
				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return DataFlavor.javaFileListFlavor.equals(flavor);
				}

				@Override
				public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
					if (!isDataFlavorSupported(flavor))
						throw new UnsupportedFlavorException(flavor);
					return Collections.singletonList(file);
				}
			};

			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);

			if (button == copyBtn) {
				copyBtn.setIcon(Icons.CHECK_GREEN.icon(24));
				Timer timer = new Timer(1500, ev -> copyBtn.setIcon(Icons.COPY.icon(24)));
				timer.setRepeats(false);
				timer.start();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void handleDesktopShare(ActionEvent e) {
		if (file == null)
			return;
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
			} else if (os.contains("mac")) {
				new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();
			} else {
				Desktop.getDesktop().open(file.getParentFile());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private String encodeForUrl(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
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