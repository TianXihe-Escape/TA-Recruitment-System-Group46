package ui.dialogs;

import model.JobPosting;
import ui.UiTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Displays a read-only summary of a job posting.
 */
public class JobDetailsDialog extends JDialog {
    public JobDetailsDialog(Frame owner, JobPosting jobPosting) {
        super(owner, "Job Details", true);
        getContentPane().setBackground(UiTheme.BACKGROUND);

        JTextArea area = new JTextArea();
        UiTheme.styleTextArea(area, 12);
        area.setEditable(false);
        area.setText(
                "Job ID: " + jobPosting.getJobId() + "\n" +
                        "Module: " + jobPosting.getModuleCode() + " - " + jobPosting.getModuleTitle() + "\n" +
                        "Duties: " + jobPosting.getDuties() + "\n" +
                        "Hours: " + jobPosting.getHours() + "\n" +
                        "Required Skills: " + String.join(", ", jobPosting.getRequiredSkills()) + "\n" +
                        "Deadline: " + jobPosting.getApplicationDeadline() + "\n" +
                        "Status: " + jobPosting.getStatus()
        );
        JScrollPane areaScrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(areaScrollPane);
        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Job Summary", "Read-only details for the selected vacancy.");
        card.add(areaScrollPane, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
        setSize(560, 420);
        setLocationRelativeTo(owner);
    }
}
