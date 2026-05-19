package ui.dialogs;

import model.ApplicationRecord;
import model.JobPosting;
import ui.UiFormat;
import ui.UiTheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Displays a read-only summary of an application together with the linked job context.
 */
public class ApplicationDetailsDialog extends JDialog {
    /**
     * Opens the dialog for one submitted application.
     */
    public ApplicationDetailsDialog(Frame owner, ApplicationRecord application, JobPosting job, String taDemandSummary) {
        this(owner, application, job, taDemandSummary, null, null);
    }

    /**
     * Opens the dialog with a TA-facing module organiser summary.
     */
    public ApplicationDetailsDialog(Frame owner,
                                    ApplicationRecord application,
                                    JobPosting job,
                                    String taDemandSummary,
                                    String moduleOrganiserSummary) {
        this(owner, application, job, taDemandSummary, moduleOrganiserSummary, null, null);
    }

    /**
     * Opens the dialog with caller-provided actions below the read-only details.
     */
    public ApplicationDetailsDialog(Frame owner,
                                    ApplicationRecord application,
                                    JobPosting job,
                                    String taDemandSummary,
                                    String moduleOrganiserSummary,
                                    JComponent actionPanel) {
        this(owner, application, job, taDemandSummary, moduleOrganiserSummary, actionPanel, null);
    }

    /**
     * Opens the dialog with optional admin management actions below the details.
     */
    public ApplicationDetailsDialog(Frame owner,
                                    ApplicationRecord application,
                                    JobPosting job,
                                    String taDemandSummary,
                                    BooleanSupplier deleteAction) {
        this(owner, application, job, taDemandSummary, null, null, deleteAction);
    }

    private ApplicationDetailsDialog(Frame owner,
                                     ApplicationRecord application,
                                     JobPosting job,
                                     String taDemandSummary,
                                     String moduleOrganiserSummary,
                                     JComponent actionPanel,
                                     BooleanSupplier deleteAction) {
        super(owner, "Application Details", true);
        getContentPane().setBackground(UiTheme.BACKGROUND);

        JPanel details = UiTheme.createFormGrid();
        int row = 0;
        UiTheme.addFormRow(details, row, "Application ID", readOnlyValue(application.getApplicationId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Job", readOnlyValue(job == null
                ? "[Deleted Job]"
                : job.getModuleCode() + " - " + job.getModuleTitle()));
        row += 2;
        if (moduleOrganiserSummary != null && !moduleOrganiserSummary.isBlank()) {
            UiTheme.addFormRow(details, row, "Module Organiser", readOnlyValue(moduleOrganiserSummary));
            row += 2;
        }
        UiTheme.addFormRow(details, row, "Job ID", readOnlyValue(application.getJobId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Status", readOnlyValue(String.valueOf(application.getStatus())));
        row += 2;
        UiTheme.addFormRow(details, row, "Applied At", readOnlyValue(UiFormat.dateTime(application.getAppliedAt())));
        row += 2;
        UiTheme.addFormRow(details, row, "Last Updated", readOnlyValue(UiFormat.dateTime(application.getLastUpdatedAt())));
        row += 2;
        UiTheme.addFormRow(details, row, "Decision At", readOnlyValue(UiFormat.dateTime(application.getDecisionAt())));
        row += 2;
        UiTheme.addFormRow(details, row, "Match Score", readOnlyValue(application.getMatchScore() + "%"));
        row += 2;
        UiTheme.addFormRow(details, row, "Missing Skills", readOnlyBlock(joinSkills(application.getMissingSkills()), 3));
        row += 2;
        UiTheme.addFormRow(details, row, "Suggestion", readOnlyBlock(buildMissingSkillSuggestion(application), 3));
        row += 2;
        UiTheme.addFormRow(details, row, "Reviewer Notes", readOnlyBlock(reviewerNotesOrPending(application.getReviewerNotes()), 4));
        row += 2;
        UiTheme.addFormRow(details, row, "TA Demand", readOnlyValue(valueOrDash(taDemandSummary)));
        row += 2;
        UiTheme.addFormRow(details, row, "Job Type", readOnlyValue(job == null ? "-" : UiFormat.valueOrDash(job.getJobType())));
        row += 2;
        UiTheme.addFormRow(details, row, "Period", readOnlyValue(job == null ? "-" : UiFormat.period(job)));
        row += 2;
        UiTheme.addFormRow(details, row, "Schedule", readOnlyValue(job == null ? "-" : UiFormat.valueOrDash(job.getSchedule())));
        row += 2;
        UiTheme.addFormRow(details, row, "Location", readOnlyValue(job == null ? "-" : UiFormat.valueOrDash(job.getLocation())));
        row += 2;
        UiTheme.addFormRow(details, row, "Workload", readOnlyValue(job == null ? "-" : UiFormat.workload(job)));
        row += 2;
        UiTheme.addFormRow(details, row, "Deadline", readOnlyValue(job == null ? "-" : UiFormat.date(job.getApplicationDeadline())));

        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Application Summary", "Read-only details for the selected application and its linked job.");
        card.add(UiTheme.wrapPage(details), BorderLayout.CENTER);
        if (actionPanel != null) {
            card.add(actionPanel, BorderLayout.SOUTH);
        } else if (deleteAction != null) {
            card.add(buildManagementButtons(application, deleteAction), BorderLayout.SOUTH);
        }
        root.add(card, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);

        setSize(760, 680);
        setMinimumSize(new Dimension(660, 560));
        setLocationRelativeTo(owner);
    }

    private JPanel buildManagementButtons(ApplicationRecord application, BooleanSupplier deleteAction) {
        JButton deleteButton = UiTheme.createDangerButton("Delete Application");
        JButton closeButton = UiTheme.createSecondaryButton("Close");
        deleteButton.addActionListener(event -> {
            String message = "Are you sure you want to delete this application?\n"
                    + application.getApplicationId()
                    + "\n\nThis will also remove related allocations and messages.";
            if (!UiMessage.confirm(this, message, "Confirm Application Deletion")) {
                return;
            }
            if (deleteAction.getAsBoolean()) {
                dispose();
            }
        });
        closeButton.addActionListener(event -> dispose());
        return UiTheme.createButtonRow(FlowLayout.RIGHT, deleteButton, closeButton);
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

    private String joinSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "-";
        }
        return String.join(", ", skills);
    }

    private String buildMissingSkillSuggestion(ApplicationRecord application) {
        if (application.getMissingSkills() == null || application.getMissingSkills().isEmpty()) {
            return "No missing skills were identified for this application.";
        }
        return "Consider strengthening or documenting: " + String.join(", ", application.getMissingSkills()) + ".";
    }

    private String reviewerNotesOrPending(String value) {
        return value == null || value.isBlank() ? "Not yet reviewed" : value;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
