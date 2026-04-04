package ui.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Small wrapper around Swing message dialogs for consistent UI feedback.
 */
public final class UiMessage {
    private UiMessage() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
