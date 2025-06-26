package com.oranbyte.screenrec.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

import com.oranbyte.screenrec.constants.AppConstant;

public class ControlFrame extends JFrame {
    public ControlFrame(MainFrame mainFrame) {
        setTitle("Controls");
        setIconImage(new ImageIcon(getClass().getResource(AppConstant.APP_LOGO)).getImage());
        setAlwaysOnTop(true);
        setSize(300, 80);
        setLayout(new FlowLayout());
        setLocation(100, 100);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JButton button = new JButton("Start");
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(Color.GREEN.darker());
        button.setForeground(Color.WHITE);
        button.setFocusable(false);
        button.addActionListener(e -> mainFrame.toggleRecording(button));

        add(button);
        setVisible(true);
    }
}
