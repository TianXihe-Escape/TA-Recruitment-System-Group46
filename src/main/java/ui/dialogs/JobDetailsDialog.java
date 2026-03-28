package ui.dialogs;

import model.JobPosting;

import javax.swing.*;
import java.awt.*;

/**
 * Displays a read-only summary of a job posting.
 */
public class JobDetailsDialog extends JDialog {
    public JobDetailsDialog(Frame owner, JobPosting jobPosting) {
        super(owner, "Job Details", true);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(
                "Job ID: " + jobPosting.getJobId() + "\n" +
                        "Module: " + jobPosting.getModuleCode() + " - " + jobPosting.getModuleTitle() + "\n" +
                        "Duties: " + jobPosting.getDuties() + "\n" +
                        "Hours: " + jobPosting.getHours() + "\n" +
                        "Required Skills: " + String.join(", ", jobPosting.getRequiredSkills()) + "\n" +
                        "Deadline: " + jobPosting.getApplicationDeadline() + "\n" +
                        "Status: " + jobPosting.getStatus()
        );
        add(new JScrollPane(area), BorderLayout.CENTER);
        setSize(420, 300);
        setLocationRelativeTo(owner);
    }
}
