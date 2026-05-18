package ui.dialogs;

import model.JobPosting;
import ui.UiFormat;
import ui.UiTheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Displays a read-only summary of a job posting.
 *
 * The dialog is intentionally simple: it does not allow editing, and it keeps
 * all job details in one scrollable text block so callers can reuse it from
 * different dashboards without needing a custom layout for each context.
 */
public class JobDetailsDialog extends JDialog {
    /**
     * Opens the dialog without a TA demand summary.
     *
     * This overload is used by screens that only need the job metadata itself.
     */
    public JobDetailsDialog(Frame owner, JobPosting jobPosting) {
        this(owner, jobPosting, null);
    }

    /**
     * Opens the dialog and optionally appends a compact TA demand summary.
     *
     * The extra summary is useful for list views that already know how many
     * applicants or accepted TAs are attached to the job.
     */
    public JobDetailsDialog(Frame owner, JobPosting jobPosting, String taDemandSummary) {
        super(owner, "Job Details", true);
        getContentPane().setBackground(UiTheme.BACKGROUND);

        JPanel details = UiTheme.createFormGrid();
        int row = 0;
        UiTheme.addFormRow(details, row, "Job ID", readOnlyValue(jobPosting.getJobId()));
        row += 2;
        UiTheme.addFormRow(details, row, "Module", readOnlyValue(jobPosting.getModuleCode() + " - " + jobPosting.getModuleTitle()));
        row += 2;
        UiTheme.addFormRow(details, row, "Category", readOnlyValue(jobPosting.getCategory() == null ? "-" : jobPosting.getCategory().getDisplayName()));
        row += 2;
        UiTheme.addFormRow(details, row, "Job Type", readOnlyValue(UiFormat.valueOrDash(jobPosting.getJobType())));
        row += 2;
        UiTheme.addFormRow(details, row, "Semester", readOnlyValue(valueOrDash(jobPosting.getSemester())));
        row += 2;
        UiTheme.addFormRow(details, row, "Period", readOnlyValue(UiFormat.period(jobPosting)));
        row += 2;
        UiTheme.addFormRow(details, row, "Schedule", readOnlyValue(UiFormat.valueOrDash(jobPosting.getSchedule())));
        row += 2;
        UiTheme.addFormRow(details, row, "Location", readOnlyValue(UiFormat.valueOrDash(jobPosting.getLocation())));
        row += 2;
        UiTheme.addFormRow(details, row, "Workload", readOnlyValue(UiFormat.workload(jobPosting)));
        row += 2;
        UiTheme.addFormRow(details, row, "Workload Type", readOnlyValue(UiFormat.valueOrDash(jobPosting.getWorkloadType())));
        row += 2;
        UiTheme.addFormRow(details, row, "TA Needed", readOnlyValue(String.valueOf(jobPosting.getRequiredTaCount())));
        row += 2;
        if (taDemandSummary != null && !taDemandSummary.isBlank()) {
            UiTheme.addFormRow(details, row, "Applications / Needed", readOnlyValue(taDemandSummary));
            row += 2;
        }
        UiTheme.addFormRow(details, row, "Deadline", readOnlyValue(UiFormat.date(jobPosting.getApplicationDeadline())));
        row += 2;
        UiTheme.addFormRow(details, row, "Status", readOnlyValue(String.valueOf(jobPosting.getStatus())));
        row += 2;
        UiTheme.addFormRow(details, row, "Required Skills", readOnlyBlock(joinSkills(jobPosting.getRequiredSkills()), 3));
        row += 2;
        UiTheme.addFormRow(details, row, "Duties", readOnlyBlock(valueOrDash(jobPosting.getDuties()), 5));

        JScrollPane detailsScrollPane = UiTheme.wrapPage(details);

        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Job Summary", "Read-only details for the selected vacancy, arranged for quicker scanning.");
        card.add(detailsScrollPane, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
        // A wider default gives field labels, module titles, and duties more
        // breathing room than the old plain text block.
        setSize(720, 620);
        setMinimumSize(new Dimension(640, 520));
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

    private String joinSkills(List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return "-";
        }
        return String.join(", ", requiredSkills);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
