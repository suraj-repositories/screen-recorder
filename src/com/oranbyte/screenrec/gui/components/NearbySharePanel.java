package com.oranbyte.screenrec.gui.components;

import java.awt.Component;
import java.awt.Dimension; 
import java.io.File;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.gui.ShareDialog;
import com.oranbyte.screenrec.share.FileShareManager;
import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendProvider;

public class NearbySharePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final ShareDialog dialog;
    private final File file; 
    private FileShareManager manager;

    public NearbySharePanel(ShareDialog dialog, File file) {
        this.dialog = dialog;
        this.file = file;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(22, 22, 22, 22));
        setBackground(ShareDialog.BG);
        
        FileShareProvider provider = new LocalSendProvider();
        manager  = new FileShareManager(provider);
         
        initUI();
        loadDevices();
    }

    private void initUI() {
        JButton backBtn = new JButton("← Back");
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.setFont(AppConstant.APP_FONT.deriveFont(14f));
        backBtn.addActionListener(e -> dialog.showMainView());
        backBtn.setFocusable(false);
        add(backBtn);

        add(Box.createVerticalStrut(20));

        JLabel header = new JLabel("Nearby Sharing");
        header.setFont(AppConstant.APP_FONT.deriveFont(18f));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(header);

        add(Box.createVerticalStrut(10));

        String fileName = (file != null) ? file.getName() : "No file selected";
        JLabel detail = new JLabel("<html>Searching for nearby receivers for:<br><b>" + fileName + "</b></html>");
        detail.setFont(AppConstant.APP_FONT.deriveFont(13f));
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.setMaximumSize(new Dimension(416, 60));
        add(detail);
    }
    
    private void loadDevices() {
    	List<ShareDevice> devices = manager.getDevices();
    	
    	System.out.println(devices); 
    }
}