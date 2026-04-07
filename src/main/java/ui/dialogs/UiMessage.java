package ui.dialogs;

import ui.UiTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Small wrapper around Swing message dialogs for consistent UI feedback.
 */
public final class UiMessage {
    private static final int MIN_COLUMNS = 28;
    private static final int MAX_COLUMNS = 52;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 3;

    private UiMessage() {
    }

    public static void info(Component parent, String message) {
        show(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        show(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(
                parent,
                buildMessagePane(message),
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        ) == JOptionPane.YES_OPTION;
    }

    private static void show(Component parent, String message, String title, int type) {
        JOptionPane.showMessageDialog(parent, buildMessagePane(message), title, type);
    }

    private static JScrollPane buildMessagePane(String message) {
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setOpaque(false);
        area.setWrapStyleWord(false);
        area.setLineWrap(false);
        area.setForeground(UiTheme.TEXT);
        area.setFont(UiTheme.uiFont(Font.PLAIN, 14));
        area.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        area.setColumns(preferredColumns(message));
        area.setRows(preferredRows(message));

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(area.getPreferredSize());
        UiTheme.styleDialogScrollPane(scrollPane);
        return scrollPane;
    }

    private static int preferredColumns(String message) {
        int longestLine = 0;
        for (String line : message.split("\\R", -1)) {
            longestLine = Math.max(longestLine, line.length());
        }
        return Math.max(MIN_COLUMNS, Math.min(MAX_COLUMNS, longestLine + 2));
    }

    private static int preferredRows(String message) {
        int lineCount = message.split("\\R", -1).length;
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, lineCount));
    }
}
