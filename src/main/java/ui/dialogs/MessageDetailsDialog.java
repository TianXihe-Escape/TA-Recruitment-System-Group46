package ui.dialogs;

import model.JobPosting;
import model.MessageRecord;
import ui.UiFormat;
import ui.UiTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Displays a read-only summary of one TA/MO message thread item.
 */
public class MessageDetailsDialog extends JDialog {
    public MessageDetailsDialog(Frame owner, MessageRecord message, JobPosting job, String direction, String status) {
        super(owner, "Message Details", true);
        getContentPane().setBackground(UiTheme.BACKGROUND);

        JPanel details = UiTheme.createFormGrid();
        int row = 0;
        UiTheme.addFormRow(details, row, "Message ID", readOnlyValue(message.getMessageId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Time", readOnlyValue(UiFormat.dateTime(message.getCreatedAt())));
        row += 2;
        UiTheme.addFormRow(details, row, "Job", readOnlyValue(job == null
                ? valueOrDash(message.getJobId())
                : job.getModuleCode() + " - " + job.getModuleTitle()));
        row += 2;
        UiTheme.addFormRow(details, row, "Direction", readOnlyValue(direction));
        row += 2;
        UiTheme.addFormRow(details, row, "Status", readOnlyValue(status));
        row += 2;
        UiTheme.addFormRow(details, row, "Application ID", readOnlyValue(message.getApplicationId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Sender User ID", readOnlyValue(message.getSenderUserId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Recipient User ID", readOnlyValue(message.getRecipientUserId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Message", readOnlyBlock(message.getBody(), 8));

        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Message Summary", "Read-only details for the selected TA/MO message.");
        card.add(UiTheme.wrapPage(details), BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);

        setSize(760, 640);
        setMinimumSize(new Dimension(660, 540));
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
