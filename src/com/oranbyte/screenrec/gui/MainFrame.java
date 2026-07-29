package com.oranbyte.screenrec.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.oranbyte.screenrec.constants.AppColors;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.CaptureMode;
import com.oranbyte.screenrec.constants.Icons;
import com.oranbyte.screenrec.constants.RecordingMode;
import com.oranbyte.screenrec.gui.components.ImageSwitch;
import com.oranbyte.screenrec.gui.components.ToolbarButton;
import com.oranbyte.screenrec.gui.components.ToolbarComboBox;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final String PAGE_EMPTY = "EMPTY";
	private static final String PAGE_CONTENT = "CONTENT";

	private SelectionFrame selectionFrame;

	private final CardLayout cardLayout = new CardLayout();
	private JPanel panel;
	private JPanel emptyPanel;
	private JPanel contentPanel;

	public MainFrame() {
		init();
	}

	private void init() {

		setTitle("Screen Recorder");
		setIconImage(Icons.FAVICON.icon().getImage());
		setAlwaysOnTop(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		setMinimumSize(new Dimension(600, 300));
		setSize(600, 300);
		setLocationRelativeTo(null);

		add(initToolbar(), BorderLayout.NORTH);

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
		captureMode.setMaximumSize(new Dimension(250, captureMode.getPreferredSize().height));

		ImageSwitch modeSwitch = new ImageSwitch(Icons.CAMERA.icon(24), Icons.VIDEO.icon(24));
		modeSwitch.setMaximumSize(new Dimension(100, modeSwitch.getPreferredSize().height));

		newButton.addActionListener(e -> {

			setVisible(false);

			Timer timer = new Timer(300, ev -> {

				if (selectionFrame == null) {
					selectionFrame = new SelectionFrame();
				}

				CaptureMode mode = (CaptureMode) captureMode.getSelectedItem();
				RecordingMode rMode = modeSwitch.getRecordingMode();

				selectionFrame.activate(this);
				selectionFrame.setCaptureMode(mode);
				selectionFrame.setRecordingMode(rMode);

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

	public void setPanelContent(Component component) {

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
			resizeWindow(size.width, size.height);
		});

		setPanelContent(player);
	}

	private void resizeWindow(int videoWidth, int videoHeight) {
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
}