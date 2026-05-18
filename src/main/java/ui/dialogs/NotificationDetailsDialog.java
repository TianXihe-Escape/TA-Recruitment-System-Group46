package ui.dialogs;

import model.NotificationRecord;
import ui.UiFormat;
import ui.UiTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Displays a read-only summary of one notification.
 */
public class NotificationDetailsDialog extends JDialog {
    public NotificationDetailsDialog(Frame owner, NotificationRecord notification) {
        super(owner, "Notification Details", true);
        getContentPane().setBackground(UiTheme.BACKGROUND);

        JPanel details = UiTheme.createFormGrid();
        int row = 0;
        UiTheme.addFormRow(details, row, "Notification ID", readOnlyValue(notification.getNotificationId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Time", readOnlyValue(UiFormat.dateTime(notification.getCreatedAt())));
        row += 2;
        UiTheme.addFormRow(details, row, "Status", readOnlyValue(notification.isRead() ? "Read" : "New"));
        row += 2;
        UiTheme.addFormRow(details, row, "Message", readOnlyBlock(notification.getMessage(), 8));

        JButton closeButton = UiTheme.createSecondaryButton("Close");
        closeButton.addActionListener(event -> dispose());

        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Notification Summary", "Read-only details for the selected notification.");
        card.add(UiTheme.wrapPage(details), BorderLayout.CENTER);
        card.add(UiTheme.createButtonRow(FlowLayout.RIGHT, closeButton), BorderLayout.SOUTH);
        root.add(card, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);

        setSize(700, 520);
        setMinimumSize(new Dimension(600, 440));
        setLocationRelativeTo(owner);
    }

    private JComponent readOnlyValue(String value) {
        JLabel label = new JLabel(valueOrDash(value));
        label.setOpaque(true);
        label.setBackground(UiTheme.SURFACE_ALT);
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.uiFont(Font.PLAIN, 14));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        return label;
    }

    private JComponent readOnlyBlock(String value, int rows) {
        JTextArea area = new JTextArea(valueOrDash(value), rows, 24);
        UiTheme.styleTextArea(area, rows);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(UiTheme.SURFACE_ALT);
        area.setForeground(UiTheme.TEXT);
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER, 1, true));
        return scrollPane;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
