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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.time.LocalDate;

/**
 * TA dashboard for profile editing, browsing jobs, and tracking applications.
 */
public class TADashboardFrame extends JFrame {
    private static final String CV_FILE_DESCRIPTION = "CV Files (*.pdf, *.doc, *.docx, *.rtf, *.txt)";

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
    private final JButton chooseCvButton = UiTheme.createSecondaryButton("Choose File");
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Hours", "TA Demand", "Deadline", "Skills", "Status"}, 0);
    private final JTable jobTable = new JTable(jobTableModel);
    private final DefaultTableModel applicationTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Job ID", "Status", "Match %", "Missing Skills", "Reviewer Notes"}, 0);
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
        UiTheme.addFormRow(form, 14, "CV Path", buildCvPathPicker());

        JButton saveProfileButton = UiTheme.createPrimaryButton("Save Profile");
        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");

        backButton.addActionListener(event -> returnToLogin());
        saveProfileButton.addActionListener(event -> saveProfile());
        chooseCvButton.addActionListener(event -> chooseCvFile());
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
        JButton viewApplicationButton = UiTheme.createSecondaryButton("View Application Details");
        JButton applyButton = UiTheme.createPrimaryButton("Apply");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh Tables");

        viewDetailsButton.addActionListener(event -> viewSelectedJob());
        viewApplicationButton.addActionListener(event -> viewSelectedApplication());
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
        body.add(UiTheme.createButtonRow(FlowLayout.LEFT, viewDetailsButton, viewApplicationButton, applyButton, refreshButton), BorderLayout.NORTH);
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
        for (JobPosting job : jobService.getOpenJobs()) {
            jobTableModel.addRow(new Object[]{
                    job.getJobId(),
                    job.getModuleCode() + " - " + job.getModuleTitle(),
                    job.getHours(),
                    buildTaDemandText(job),
                    job.getApplicationDeadline(),
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
                    String.join(", ", record.getMissingSkills()),
                    valueOrDash(record.getReviewerNotes())
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
        JobPosting job = jobService.getJobById(jobId);
        String taDemandSummary = buildTaDemandText(job);
        new JobDetailsDialog(this, job, taDemandSummary).setVisible(true);
    }

    private void viewSelectedApplication() {
        int row = applicationTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an application first.");
            return;
        }

        String applicationId = String.valueOf(applicationTableModel.getValueAt(row, 0));
        ApplicationRecord application = applicationService.getApplicationsForApplicant(profile.getApplicantId()).stream()
                .filter(record -> applicationId.equals(record.getApplicationId()))
                .findFirst()
                .orElse(null);
        if (application == null) {
            UiMessage.error(this, "Application details could not be found.");
            return;
        }

        JobPosting job = jobService.getJobById(application.getJobId());
        // Keep the popup text compact but complete so a TA can understand the application outcome without
        // switching between the table, job details dialog, and profile panel.
        String details = "Application ID: " + application.getApplicationId() + "\n" +
                "Job: " + job.getModuleCode() + " - " + job.getModuleTitle() + "\n" +
                "Status: " + application.getStatus() + "\n" +
                "Applied At: " + valueOrDash(application.getAppliedAt() == null ? null : application.getAppliedAt().toString()) + "\n" +
                "Match Score: " + application.getMatchScore() + "%\n" +
                "Missing Skills: " + valueOrDash(String.join(", ", application.getMissingSkills())) + "\n" +
                "Reviewer Notes: " + valueOrDash(application.getReviewerNotes()) + "\n" +
                "TA Demand: " + buildTaDemandText(job) + "\n" +
                "Deadline: " + valueOrDash(job.getApplicationDeadline() == null ? null : job.getApplicationDeadline().toString());
        UiMessage.info(this, details);
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

    private JPanel buildCvPathPicker() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.add(cvPathField, BorderLayout.CENTER);
        panel.add(chooseCvButton, BorderLayout.EAST);
        return panel;
    }

    private void chooseCvFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select CV File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
                CV_FILE_DESCRIPTION,
                "pdf", "doc", "docx", "rtf", "txt"
        ));
        UiTheme.styleFileChooser(chooser);

        String existingPath = cvPathField.getText().trim();
        if (!existingPath.isBlank()) {
            File existingFile = new File(existingPath);
            if (existingFile.exists()) {
                chooser.setSelectedFile(existingFile);
            }
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            File selectedFile = chooser.getSelectedFile();
            String selectedPath = selectedFile.getAbsolutePath();
            if (!validationService.validateCvPath(selectedPath).isEmpty()) {
                UiMessage.error(this, "Please choose a CV in PDF, DOC, DOCX, RTF, or TXT format.");
                return;
            }
            cvPathField.setText(selectedPath);
        }
    }

    private void styleComponents() {
        UiTheme.styleTextField(nameField);
        UiTheme.styleTextField(emailField);
        UiTheme.styleTextField(phoneField);
        UiTheme.styleTextField(skillsField);
        UiTheme.styleTextField(availabilityField);
        UiTheme.styleTextField(preferredDutiesField);
        UiTheme.styleTextField(cvPathField);
        cvPathField.setEditable(false);
        cvPathField.setToolTipText("Choose your CV file to fill this path automatically.");
        UiTheme.styleTextArea(experienceArea, 5);
        UiTheme.styleTable(jobTable);
        UiTheme.styleTable(applicationTable);
        jobTable.getColumnModel().getColumn(4).setCellRenderer(new DeadlineWarningRenderer());
        UiTheme.setColumnWidths(jobTable, 90, 280, 80, 100, 130, 220, 100);
        UiTheme.setColumnWidths(applicationTable, 120, 90, 120, 90, 220, 280);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String buildTaDemandText(JobPosting job) {
        // TA demand is presented as accepted/required so the applicant sees remaining competition at a glance.
        int acceptedCount = applicationService.getAcceptedCountForJob(job.getJobId());
        return Math.min(acceptedCount, job.getRequiredTaCount()) + "/" + job.getRequiredTaCount();
    }

    private JScrollPane wrapArea(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
    }

    private static class DeadlineWarningRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value instanceof LocalDate deadline) {
                // This gives a lightweight deadline warning without adding extra columns or dialogs.
                long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline);
                if (deadline.isBefore(LocalDate.now())) {
                    component.setBackground(new Color(255, 224, 224));
                } else if (daysRemaining <= 3) {
                    component.setBackground(new Color(255, 245, 204));
                } else {
                    component.setBackground(Color.WHITE);
                }
            } else if (!isSelected) {
                component.setBackground(Color.WHITE);
            }
            return component;
        }
    }
}
