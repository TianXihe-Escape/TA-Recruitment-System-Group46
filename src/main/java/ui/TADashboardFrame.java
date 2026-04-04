package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.JobPosting;
import model.User;
import service.ApplicantService;
import service.ApplicationService;
import service.DataService;
import service.JobService;
import service.MatchingService;
import service.ValidationService;
import ui.dialogs.JobDetailsDialog;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * TA dashboard for profile editing, browsing jobs, and tracking applications.
 */
public class TADashboardFrame extends JFrame {
    private final DataService dataService;
    private final ApplicantService applicantService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final ValidationService validationService;
    private final User currentUser;

    private ApplicantProfile profile;
    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField skillsField = new JTextField();
    private final JTextField availabilityField = new JTextField();
    private final JTextField preferredDutiesField = new JTextField();
    private final JTextArea experienceArea = new JTextArea(3, 20);
    private final JTextField cvPathField = new JTextField();
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Hours", "Skills", "Status"}, 0);
    private final JTable jobTable = new JTable(jobTableModel);
    private final DefaultTableModel applicationTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Job ID", "Status", "Match %", "Missing Skills"}, 0);
    private final JTable applicationTable = new JTable(applicationTableModel);

    public TADashboardFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;
        this.validationService = new ValidationService();
        this.applicantService = new ApplicantService(dataService.getProfileRepository(), validationService);
        this.jobService = new JobService(dataService.getJobRepository(), validationService);
        this.applicationService = new ApplicationService(
                dataService.getApplicationRepository(),
                dataService.getJobRepository(),
                new MatchingService()
        );
        this.profile = applicantService.getProfileByUserId(currentUser.getUserId());

        setTitle("TA Dashboard - " + Constants.APP_TITLE);
        setSize(1150, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        splitPane.setResizeWeight(0.4);
        add(splitPane);

        loadProfile();
        refreshJobs();
        refreshApplications();
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));
        form.add(new JLabel("Name"));
        form.add(nameField);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Phone"));
        form.add(phoneField);
        form.add(new JLabel("Skills"));
        form.add(skillsField);
        form.add(new JLabel("Availability"));
        form.add(availabilityField);
        form.add(new JLabel("Preferred Duties"));
        form.add(preferredDutiesField);
        form.add(new JLabel("Experience Summary"));
        form.add(new JScrollPane(experienceArea));
        form.add(new JLabel("CV Path"));
        form.add(cvPathField);

        JButton saveProfileButton = new JButton("Save Profile");
        JButton backButton = new JButton("Back to Login");
        JButton refreshButton = new JButton("Refresh");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(backButton);
        buttons.add(refreshButton);
        buttons.add(saveProfileButton);

        backButton.addActionListener(event -> returnToLogin());
        saveProfileButton.addActionListener(event -> saveProfile());
        refreshButton.addActionListener(event -> {
            profile = applicantService.getProfileByUserId(currentUser.getUserId());
            loadProfile();
            refreshJobs();
            refreshApplications();
        });

        panel.add(form, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton viewDetailsButton = new JButton("View Job Details");
        JButton applyButton = new JButton("Apply");
        JButton refreshButton = new JButton("Refresh Tables");

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topButtons.add(viewDetailsButton);
        topButtons.add(applyButton);
        topButtons.add(refreshButton);

        viewDetailsButton.addActionListener(event -> viewSelectedJob());
        applyButton.addActionListener(event -> applyForSelectedJob());
        refreshButton.addActionListener(event -> {
            refreshJobs();
            refreshApplications();
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Available Jobs", new JScrollPane(jobTable));
        tabs.addTab("My Applications", new JScrollPane(applicationTable));

        panel.add(topButtons, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private void loadProfile() {
        nameField.setText(profile.getName());
        emailField.setText(profile.getEmail());
        phoneField.setText(profile.getPhone());
        skillsField.setText(String.join(", ", profile.getSkills()));
        availabilityField.setText(profile.getAvailability());
        preferredDutiesField.setText(profile.getPreferredDuties());
        experienceArea.setText(profile.getExperienceSummary());
        cvPathField.setText(profile.getCvPath());
    }

    private void saveProfile() {
        try {
            profile.setName(nameField.getText().trim());
            profile.setEmail(emailField.getText().trim());
            profile.setPhone(phoneField.getText().trim());
            profile.setSkills(validationService.parseSkills(skillsField.getText()));
            profile.setAvailability(availabilityField.getText().trim());
            profile.setPreferredDuties(preferredDutiesField.getText().trim());
            profile.setExperienceSummary(experienceArea.getText().trim());
            profile.setCvPath(cvPathField.getText().trim());
            applicantService.saveProfile(profile);
            UiMessage.info(this, "Profile saved successfully.");
            refreshApplications();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    private void refreshJobs() {
        jobTableModel.setRowCount(0);
        for (JobPosting job : jobService.getAllJobs()) {
            jobTableModel.addRow(new Object[]{
                    job.getJobId(),
                    job.getModuleCode() + " - " + job.getModuleTitle(),
                    job.getHours(),
                    String.join(", ", job.getRequiredSkills()),
                    job.getStatus()
            });
        }
    }

    private void refreshApplications() {
        applicationTableModel.setRowCount(0);
        for (ApplicationRecord record : applicationService.getApplicationsForApplicant(profile.getApplicantId())) {
            applicationTableModel.addRow(new Object[]{
                    record.getApplicationId(),
                    record.getJobId(),
                    record.getStatus(),
                    record.getMatchScore(),
                    String.join(", ", record.getMissingSkills())
            });
        }
    }

    private void viewSelectedJob() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select a job first.");
            return;
        }
        String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
        new JobDetailsDialog(this, jobService.getJobById(jobId)).setVisible(true);
    }

    private void applyForSelectedJob() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select a job first.");
            return;
        }

        try {
            String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
            JobPosting job = jobService.getJobById(jobId);
            ApplicationRecord record = applicationService.apply(profile, job);
            UiMessage.info(this, "Application submitted.\nMatch score: " + record.getMatchScore() + "%");
            refreshApplications();
            refreshJobs();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    private void returnToLogin() {
        new LoginFrame(dataService).setVisible(true);
        dispose();
    }
}
