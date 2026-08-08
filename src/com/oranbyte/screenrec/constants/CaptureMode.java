package com.oranbyte.screenrec.constants;

import javax.swing.Icon;

public enum CaptureMode {

    RECTANGLE("Rectangle", Icons.RECTANGLE.icon(24)),
    WINDOW("Window", Icons.WINDOW.icon(24)),
    ENTIRE_SCREEN("Entire Screen", Icons.ENTIRE_SCREEN.icon(24));

    private final String displayName;
    private final Icon icon;

    CaptureMode(String displayName, Icon icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Icon getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

