package com.oranbyte.screenrec.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;

import com.oranbyte.screenrec.constants.Icons;

public class ShareDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final File file;

    private static final Color BG     = new Color(246, 247, 251);
    private static final Color CARD   = Color.WHITE;
    private static final Color BORDER = new Color(228, 230, 236);
    private static final Color TEXT   = new Color(30, 30, 34);
    private static final Color MUTED  = new Color(130, 130, 138);

    private ToolbarButton copyBtn;

    public ShareDialog(Frame owner, File file) {
        super(owner, "Share File", true);
        this.file = file;
        initUI();
    }

    private void initUI() {
        setSize(480, 520);
        setResizable(false);
        setLocationRelativeTo(getOwner());
        getContentPane().setBackground(BG);
        setIconImage(Icons.SHARE.icon(32).getImage());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(22, 22, 22, 22));
        root.setBackground(BG);

        addFileCard(root);
        root.add(Box.createVerticalStrut(18));

        addNearbyShareButton(root);
        root.add(Box.createVerticalStrut(18));

        addShareButtons(root);
        root.add(Box.createVerticalStrut(18));

        addActions(root);

        add(root, BorderLayout.CENTER);
    }
 
    private void addFileCard(JPanel root) {
        RoundedPanel card = new RoundedPanel(14);
        card.setLayout(new BorderLayout(14, 0));
        card.setBackground(CARD);
        card.setBorderColor(BORDER);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setPreferredSize(new Dimension(0, 72));

        JLabel icon = new JLabel(scaleIcon(resolveFileIcon(), 32));
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
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        name.setForeground(TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        String fileSize = "";
        if (file != null) {
            double mb = file.length() / 1024d / 1024d;
            fileSize = mb >= 1
                    ? String.format("%.2f MB", mb)
                    : String.format("%.0f KB", file.length() / 1024d);
        }

        JLabel size = new JLabel(fileSize);
        size.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
        copyBtn.setToolTipText("Copy file to clipboard (paste into chats)");
        copyBtn.addActionListener(e -> handleCopyFile(copyBtn));

        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setOpaque(false);
        actionPanel.add(copyBtn);

        card.add(icon, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.EAST);

        root.add(card);
    } 
    
    private void addNearbyShareButton(JPanel root) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String label;
        String tip;

        if (os.contains("win")) {
            label = "Nearby Share / Windows Share";
            tip   = "Opens the Windows Share sheet (Nearby Sharing, Email, apps…)";
        } else if (os.contains("mac")) {
            label = "AirDrop / Share…";
            tip   = "Opens Finder share / AirDrop";
        } else {
            label = "Share via System…";
            tip   = "Opens system share options or folder";
        }

        ToolbarButton btn = createActionButton(label);
        btn.setToolTipText(tip);
        btn.addActionListener(e -> handleSystemShare());
        root.add(btn);
    }

    private void handleSystemShare() {
    	
        if (file == null || !file.exists()) return;

        String os = System.getProperty("os.name", "").toLowerCase();

        try {
            if (os.contains("win")) {
            	System.out.println("shareing..."); 
                String parent = file.getParentFile().getAbsolutePath().replace("'", "''");
                String name   = file.getName().replace("'", "''");

                String ps = String.format(
                    "$shell = New-Object -ComObject Shell.Application; " +
                    "$folder = $shell.NameSpace('%s'); " +
                    "$item = $folder.ParseName('%s'); " +
                    "if ($item) { $item.InvokeVerb('Share') }",
                    parent, name);

                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", ps);
                pb.redirectErrorStream(true);
                pb.start();

            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-R", file.getAbsolutePath()).start();

                String script =
                    "tell application \"Finder\"\n" +
                    "  activate\n" +
                    "  set theFile to (POSIX file \"" + file.getAbsolutePath() + "\") as alias\n" +
                    "  share theFile\n" +
                    "end tell";
                new ProcessBuilder("osascript", "-e", script).start();

            } else {
                boolean sent = false;
                try {
                    Process p = new ProcessBuilder(
                            "kdeconnect-cli", "--share", file.getAbsolutePath()).start();
                    if (p.waitFor() == 0) sent = true;
                } catch (Exception ignored) {}

                if (!sent) {
                    Desktop.getDesktop().open(file.getParentFile());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            handleDesktopShare(null);
        }
    }

    private void addShareButtons(JPanel root) {
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);

        grid.add(createShareButton("WhatsApp", Icons.WHATSAPP, e -> shareToPlatform("whatsapp")));
        grid.add(createShareButton("Telegram", Icons.TELEGRAM, e -> shareToPlatform("telegram")));
        grid.add(createShareButton("Email",    Icons.EMAIL,    e -> shareToPlatform("email")));
        grid.add(createShareButton("Twitter / X", Icons.TWITTER, e -> shareToPlatform("twitter")));

        root.add(grid);
    }

    private ToolbarButton createShareButton(String text, Icons icon, ActionListener listener) {
        ToolbarButton button = new ToolbarButton(text, icon, 24);
        button.setFocusable(false);
        button.addActionListener(listener);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setIconTextGap(8);
        return button;
    }

    private void shareToPlatform(String platform) {
        if (file == null) return;

        handleCopyFile(copyBtn);

        try {
            String label   = "Check this file: " + file.getName();
            String message = label + "\n\n(File is also on your clipboard – just paste it)";
            String path    = file.getAbsolutePath();

            String url = switch (platform) {
                case "whatsapp" -> "https://web.whatsapp.com/send?text=" + encodeForUrl(message);
                case "telegram" -> "https://t.me/share/url?url=" + encodeForUrl(file.toURI().toString())
                                   + "&text=" + encodeForUrl(label);
                case "twitter"  -> "https://twitter.com/intent/tweet?text=" + encodeForUrl(message);
                case "email"    -> {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                        try {
                        	 
                            Desktop.getDesktop().mail(new URI("mailto:?subject=" + encodeForUrl(label)
                                    + "&body=" + encodeForUrl(message + "\n\nPath: " + path) + "&attachment=C:\\Users\\Shubham\\Desktop\\sql.png" ) );
                            yield null;  
                        } catch (Exception ignored) {}
                    }
                    yield "mailto:?subject=" + encodeForUrl(label)
                          + "&body=" + encodeForUrl(message + "\n\nPath: " + path);
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
    
    private void addActions(JPanel root) {
        ToolbarButton openFile = createActionButton("Open File");
        openFile.addActionListener(e -> {
            try {
                if (file != null) Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        ToolbarButton showInFolder = createActionButton("Show in Folder");
        showInFolder.addActionListener(this::handleDesktopShare);

        ToolbarButton copyPath = createActionButton("Copy File Path");
        copyPath.addActionListener(e -> handleCopyPath(copyPath));

        root.add(openFile);
        root.add(Box.createVerticalStrut(10));
        root.add(showInFolder);
        root.add(Box.createVerticalStrut(10));
        root.add(copyPath);
    }

    private ToolbarButton createActionButton(String text) {
        ToolbarButton button = new ToolbarButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setAlignmentX(JButton.CENTER_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        return button;
    }

    private void handleCopyPath(JButton button) {
        if (file == null) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
               .setContents(new StringSelection(file.getAbsolutePath()), null);
        animateCopied(button, "Copy File Path");
    }

    private void handleCopyFile(JButton button) {
        if (file == null) return;
        try {
            Transferable transferable = new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{ DataFlavor.javaFileListFlavor };
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
        if (file == null) return;
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

    private void animateCopied(JButton button, String originalText) {
        button.setText("Copied !");
        Timer timer = new Timer(1500, e -> button.setText(originalText));
        timer.setRepeats(false);
        timer.start();
    }

    private Icon resolveFileIcon() {
        if (file == null) return new ImageIcon();
        try {
            Icon systemIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
            if (systemIcon != null) return systemIcon;
        } catch (Exception ignored) {}
        return new ImageIcon();
    }

    private static Icon scaleIcon(Icon icon, int targetWidth) {
        if (icon == null) return null;
        int srcW = icon.getIconWidth();
        int srcH = icon.getIconHeight();
        double scale = (double) targetWidth / srcW;
        int targetHeight = (int) Math.round(srcH * scale);

        BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.scale(scale, scale);
        icon.paintIcon(null, g2, 0, 0);
        g2.dispose();
        return new ImageIcon(image);
    }

    private static class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int radius;
        private Color borderColor = BORDER;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        void setBorderColor(Color color) {
            this.borderColor = color;
        }

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
}