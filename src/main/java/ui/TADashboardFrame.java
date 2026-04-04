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
        setSize(1320, 860);
        setMinimumSize(new Dimension(980, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        styleComponents();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        splitPane.setResizeWeight(0.38);
        UiTheme.styleSplitPane(splitPane);

        JPanel root = UiTheme.createPagePanel();
        root.add(UiTheme.createHeader("TA Workspace", "Complete your profile, review open modules, and track application outcomes."), BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        add(UiTheme.wrapPage(root));

        loadProfile();
        refreshJobs();
        refreshApplications();
    }

    private JPanel buildLeftPanel() {
        JPanel panel = UiTheme.createCard("Applicant Profile", "Keep this profile current so matching and review decisions stay accurate.");

        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "Name", nameField);
        UiTheme.addFormRow(form, 2, "Email", emailField);
        UiTheme.addFormRow(form, 4, "Phone", phoneField);
        UiTheme.addFormRow(form, 6, "Skills", skillsField);
        UiTheme.addFormRow(form, 8, "Availability", availabilityField);
        UiTheme.addFormRow(form, 10, "Preferred Duties", preferredDutiesField);
        UiTheme.addFormRow(form, 12, "Experience Summary", wrapArea(experienceArea));
        UiTheme.addFormRow(form, 14, "CV Path", cvPathField);

        JButton saveProfileButton = UiTheme.createPrimaryButton("Save Profile");
        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");

        backButton.addActionListener(event -> returnToLogin());
        saveProfileButton.addActionListener(event -> saveProfile());
        refreshButton.addActionListener(event -> {
            profile = applicantService.getProfileByUserId(currentUser.getUserId());
            loadProfile();
            refreshJobs();
            refreshApplications();
        });

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UiTheme.wrapPage(form), BorderLayout.CENTER);
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, backButton, refreshButton, saveProfileButton), BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = UiTheme.createCard("Opportunities", "Browse available jobs and monitor the progress of your submissions.");

        JButton viewDetailsButton = UiTheme.createSecondaryButton("View Job Details");
        JButton applyButton = UiTheme.createPrimaryButton("Apply");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh Tables");

        viewDetailsButton.addActionListener(event -> viewSelectedJob());
        applyButton.addActionListener(event -> applyForSelectedJob());
        refreshButton.addActionListener(event -> {
            refreshJobs();
            refreshApplications();
        });

        JTabbedPane tabs = new JTabbedPane();
        UiTheme.styleTabs(tabs);
        tabs.addTab("Available Jobs", UiTheme.wrapTable(jobTable));
        tabs.addTab("My Applications", UiTheme.wrapTable(applicationTable));

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UiTheme.createButtonRow(FlowLayout.LEFT, viewDetailsButton, applyButton, refreshButton), BorderLayout.NORTH);
        body.add(tabs, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
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

    private void styleComponents() {
        UiTheme.styleTextField(nameField);
        UiTheme.styleTextField(emailField);
        UiTheme.styleTextField(phoneField);
        UiTheme.styleTextField(skillsField);
        UiTheme.styleTextField(availabilityField);
        UiTheme.styleTextField(preferredDutiesField);
        UiTheme.styleTextField(cvPathField);
        UiTheme.styleTextArea(experienceArea, 5);
        UiTheme.styleTable(jobTable);
        UiTheme.styleTable(applicationTable);
        UiTheme.setColumnWidths(jobTable, 90, 300, 80, 260, 100);
        UiTheme.setColumnWidths(applicationTable, 120, 90, 120, 90, 260);
    }

    private JScrollPane wrapArea(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
    }
}
