package ui.dialogs;

import model.JobPosting;
import ui.UiFormat;
import ui.UiTheme;

import javax.swing.*;
import java.awt.*;

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

        // A plain text area keeps the dialog easy to scan and avoids building a
        // heavier form layout for information that the user cannot edit here.
        JTextArea area = new JTextArea();
        UiTheme.styleTextArea(area, 12);
        area.setEditable(false);
        area.setText(
                "Job ID: " + jobPosting.getJobId() + "\n" +
                        "Module: " + jobPosting.getModuleCode() + " - " + jobPosting.getModuleTitle() + "\n" +
                        "Category: " + (jobPosting.getCategory() == null ? "-" : jobPosting.getCategory().getDisplayName()) + "\n" +
                        "Job Type: " + UiFormat.valueOrDash(jobPosting.getJobType()) + "\n" +
                        "Semester: " + (jobPosting.getSemester() == null || jobPosting.getSemester().isBlank() ? "-" : jobPosting.getSemester()) + "\n" +
                        "Period: " + UiFormat.period(jobPosting) + "\n" +
                        "Schedule: " + UiFormat.valueOrDash(jobPosting.getSchedule()) + "\n" +
                        "Location: " + UiFormat.valueOrDash(jobPosting.getLocation()) + "\n" +
                        "Duties: " + jobPosting.getDuties() + "\n" +
                        "Workload Type: " + UiFormat.valueOrDash(jobPosting.getWorkloadType()) + "\n" +
                        "Workload Hours: " + UiFormat.workload(jobPosting) + "\n" +
                        "TA Needed: " + jobPosting.getRequiredTaCount() + "\n" +
                        (taDemandSummary == null || taDemandSummary.isBlank() ? "" : "Applications / Needed: " + taDemandSummary + "\n") +
                        "Required Skills: " + String.join(", ", jobPosting.getRequiredSkills()) + "\n" +
                        "Deadline: " + jobPosting.getApplicationDeadline() + "\n" +
                        "Status: " + jobPosting.getStatus()
        );
        // Scrolling is important because some postings can include longer duties
        // or skill lists, especially when this dialog is opened on smaller screens.
        JScrollPane areaScrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(areaScrollPane);
        // The card wrapper keeps the dialog visually consistent with the rest of
        // the application's themed pages and dashboard components.
        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Job Summary", "Read-only details for the selected vacancy.");
        card.add(areaScrollPane, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
        // The dialog size is fixed to a comfortable default that still leaves
        // enough room for the scroll area when longer job descriptions appear.
        setSize(560, 420);
        setLocationRelativeTo(owner);
    }
}
