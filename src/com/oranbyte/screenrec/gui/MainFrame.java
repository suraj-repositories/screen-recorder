package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.AppUI;
import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.constants.RecordingMode;
import com.oranbyte.screenrec.gui.components.ImageSwitch;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.ToolbarComboBox;
import com.oranbyte.screenrec.util.NotificationUtil;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final String PAGE_EMPTY = "EMPTY";
	private static final String PAGE_CONTENT = "CONTENT";

	private final CardLayout cardLayout = new CardLayout();
	private SelectionFrame selectionFrame; 
	private JPanel panel;
	private JPanel emptyPanel;
	private JPanel contentPanel; 
	private JToolBar appToolbar; 
	private File currentFile;
	private ToolbarButton saveBtn;
	private ToolbarButton copyBtn;
	private ToolbarButton shareBtn;
	
	protected final Dimension defaultDimention; 

	public MainFrame() {
		defaultDimention = new Dimension(680, 350);
		new AppUI();
		init();
		
	}

	private void init() {

		setTitle("Screen Recorder");
		setIconImage(Icons.FAVICON.icon().getImage());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		setMinimumSize(defaultDimention);
		setSize(defaultDimention);
		setLocationRelativeTo(null);

		appToolbar = initToolbar();
		initActionButtons();
		add(appToolbar, BorderLayout.NORTH);

		panel = new JPanel(cardLayout);

		emptyPanel = createNoContentPanel();

		contentPanel = new JPanel(new BorderLayout());
		contentPanel.setOpaque(false);

		panel.add(emptyPanel, PAGE_EMPTY);
		panel.add(contentPanel, PAGE_CONTENT);

		cardLayout.show(panel, PAGE_EMPTY);

		add(panel, BorderLayout.CENTER);

		SwingUtilities.invokeLater(() -> {
			selectionFrame = new SelectionFrame();
			selectionFrame.setVisible(false);
		});

		setVisible(true);
	}

	private JToolBar initToolbar() {

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(AppColors.BACKGROUND);
		toolbar.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

		ToolbarButton newButton = new ToolbarButton("New", Icons.PLUS);

		ToolbarComboBox<CaptureMode> captureMode = new ToolbarComboBox<>(CaptureMode.values());
		captureMode.setPreferredSize(new Dimension(230, captureMode.getPreferredSize().height));
		captureMode.setMaximumSize(new Dimension(250, captureMode.getPreferredSize().height));

		ImageSwitch modeSwitch = new ImageSwitch(Icons.CAMERA.icon(24), Icons.VIDEO.icon(24));

		Dimension modeSwitchPref = modeSwitch.getPreferredSize();
		modeSwitch.setMinimumSize(modeSwitchPref);
		modeSwitch.setPreferredSize(modeSwitchPref);
		modeSwitch.setMaximumSize(new Dimension(100, modeSwitchPref.height));

		newButton.addActionListener(e -> {

			selectionFrame = new SelectionFrame();
			setVisible(false);

			Timer timer = new Timer(300, ev -> {
				CaptureMode mode = (CaptureMode) captureMode.getSelectedItem();
				RecordingMode rMode = modeSwitch.getRecordingMode();
				
				selectionFrame.activate(this);
				selectionFrame.setRecordingMode(rMode);
				selectionFrame.setCaptureMode(mode);

			});

			timer.setRepeats(false);
			timer.start();

		});

		toolbar.add(newButton);
		toolbar.add(Box.createHorizontalStrut(10));
		toolbar.add(modeSwitch);
		toolbar.add(Box.createHorizontalStrut(10));
		toolbar.add(captureMode);

		return toolbar;
	}

	private JPanel createNoContentPanel() {

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);

		JPanel center = new JPanel();
		center.setOpaque(false);
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

		JLabel titleLabel = new JLabel("No Recording Active");
		titleLabel.setFont(AppConstant.APP_FONT.deriveFont(Font.BOLD, 24f));
		titleLabel.setForeground(AppColors.TEXT);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel subtitleLabel = new JLabel("Select a capture mode and click Start to begin recording.");
		subtitleLabel.setFont(AppConstant.APP_FONT.deriveFont(14f));
		subtitleLabel.setForeground(AppColors.TEXT_SECONDARY);
		subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		center.add(titleLabel);
		center.add(Box.createVerticalStrut(8));
		center.add(subtitleLabel);

		JPanel grid = new JPanel(new java.awt.GridBagLayout());
		grid.setOpaque(false);
		grid.add(center);

		wrapper.add(grid, BorderLayout.CENTER);

		return wrapper;
	}

	public void showNoContentPanel() {
		cardLayout.show(panel, PAGE_EMPTY);
	}

	public void setPanelContent(@SuppressWarnings("exports") Component component) {

		contentPanel.removeAll();
		contentPanel.add(component, BorderLayout.CENTER);
		contentPanel.revalidate();
		contentPanel.repaint();

		cardLayout.show(panel, PAGE_CONTENT);
	}

	public void setVideoPanel(String src) {

		VideoPlayerPanel player = new VideoPlayerPanel(this);
		player.open(src);
		player.setOnVideoReady(size -> {
			resizeWindow(player, size.width, size.height);
		});
		setActionButtons(new File(src));

		setPanelContent(player);
	}

	private void resizeWindow(VideoPlayerPanel player, int videoWidth, int videoHeight) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		int maxWidth = (int) (screen.width * 0.85);
		int maxHeight = (int) (screen.height * 0.85);

		double scale = Math.min(1.0, Math.min((double) maxWidth / videoWidth, (double) maxHeight / videoHeight));

		int width = (int) (videoWidth * scale);
		int height = (int) (videoHeight * scale) + 80;

		setPreferredSize(new Dimension(width, height));
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public void initActionButtons() {
		saveBtn = new ToolbarButton(Icons.SAVE, 24);
		saveBtn.setVisible(false);
		saveBtn.addActionListener(e -> {
		    if (currentFile == null) {
		        return;
		    }
 
		    JFileChooser fileChooser = new JFileChooser(currentFile.getParentFile()) {
		        /**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
		        protected JDialog createDialog(Component parent) throws HeadlessException {
		            JDialog dialog = super.createDialog(parent);
		            dialog.setIconImage(Icons.FAVICON.icon(24).getImage());
		            return dialog;
		        }
		    };

		    fileChooser.setSelectedFile(currentFile);
		    fileChooser.setDialogTitle("Save File");
 
		    int result = fileChooser.showSaveDialog(null);

		    if (result == JFileChooser.APPROVE_OPTION) {
		        File destFile = fileChooser.getSelectedFile();

		        try {
		            Files.copy(
		                currentFile.toPath(),
		                destFile.toPath(),
		                StandardCopyOption.REPLACE_EXISTING
		            );

		            NotificationUtil.success(
		                "Save Complete",
		                "File saved successfully to:\n" + destFile.getAbsolutePath()
		            );
		        } catch (IOException ex) {
		            NotificationUtil.error(
		                "Save Error",
		                "Failed to save file: " + ex.getMessage()
		            );
		        }
		    }
		});

		copyBtn = new ToolbarButton(Icons.COPY, 24);
		copyBtn.setVisible(false);
		copyBtn.addActionListener(e -> {
			if (currentFile == null)
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
						if (!isDataFlavorSupported(flavor)) {
							throw new UnsupportedFlavorException(flavor);
						}
						return Collections.singletonList(currentFile);
					}
				};

				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);

				copyBtn.setIcon(Icons.CHECK_GREEN.icon(24));
				Timer timer = new Timer(1500, ev -> {
					copyBtn.setIcon(Icons.COPY.icon(24));
				});

				timer.setRepeats(false);
				timer.start();
			} catch (Exception ex) {
				NotificationUtil.error("Copy Error", "Failed to copy file: " + ex.getMessage());
			}
		});

		shareBtn = new ToolbarButton(Icons.SHARE, 24);
		shareBtn.setVisible(false);
		shareBtn.addActionListener(e -> {
			if (currentFile == null)
				return;

			ShareDialog shareDialog = new ShareDialog(this, currentFile);
			shareDialog.setVisible(true);

		});

		appToolbar.add(Box.createHorizontalGlue());
		appToolbar.add(saveBtn);
		appToolbar.add(Box.createHorizontalStrut(10));
		appToolbar.add(copyBtn);
		appToolbar.add(Box.createHorizontalStrut(10));
		appToolbar.add(shareBtn);
	}

	public void setActionButtons(File file) {
		this.currentFile = file;

		boolean isVisible = (file != null && file.exists());

		if (saveBtn != null)
			saveBtn.setVisible(isVisible);
		if (copyBtn != null)
			copyBtn.setVisible(isVisible);
		if (shareBtn != null)
			shareBtn.setVisible(isVisible);

		appToolbar.revalidate();
		appToolbar.repaint();
	}
}