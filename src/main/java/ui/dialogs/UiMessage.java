package ui.dialogs;

import ui.UiTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Small wrapper around Swing message dialogs for consistent UI feedback.
 */
public final class UiMessage {
    private UiMessage() {
    }

    public static void info(Component parent, String message) {
        show(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        show(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void show(Component parent, String message, String title, int type) {
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setOpaque(false);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setForeground(UiTheme.TEXT);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JOptionPane.showMessageDialog(parent, area, title, type);
    }
}
