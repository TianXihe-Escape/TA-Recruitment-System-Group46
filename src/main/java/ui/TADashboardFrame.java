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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Main TA workspace.
 * This frame combines profile editing, job browsing, and application tracking
 * so applicants can complete their full workflow from one screen.
 */
public class TADashboardFrame extends JFrame {
    /**
     * Description shown in the CV file chooser.
     */
    private static final String CV_FILE_DESCRIPTION = "CV Files (*.pdf, *.doc, *.docx, *.rtf, *.txt)";

    /**
     * Shared data facade used for repositories and navigation back to login.
     */
    private final DataService dataService;
    /**
     * Service for loading and saving applicant profiles.
     */
    private final ApplicantService applicantService;
    /**
     * Service for querying jobs.
     */
    private final JobService jobService;
    /**
     * Service for submission and application workflow actions.
     */
    private final ApplicationService applicationService;
    /**
     * Validation helper for parsing form input.
     */
    private final ValidationService validationService;
    /**
     * Current authenticated user.
     */
    private final User currentUser;

    /**
     * Cached profile for the logged-in TA.
     */
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
    private final JTable jobTable = new PlaceholderTable(jobTableModel, "No open jobs are available right now.");
    private final DefaultTableModel applicationTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Job ID", "Status", "Match %", "Missing Skills", "Reviewer Notes"}, 0);
    private final JTable applicationTable = new PlaceholderTable(applicationTableModel, "You have not submitted any applications yet.");

    /**
     * Constructs the TA dashboard and loads the initial data snapshot.
     */
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

    /**
     * Builds the profile-editing side of the split layout.
     */
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
        JButton deleteAccountButton = UiTheme.createDangerButton("Delete Account");
        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");

        deleteAccountButton.addActionListener(event -> deleteCurrentAccount());
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
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, deleteAccountButton, backButton, refreshButton, saveProfileButton), BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the jobs and applications side of the split layout.
     */
    private JPanel buildRightPanel() {
        JPanel panel = UiTheme.createCard("Opportunities", "Browse available jobs and monitor the progress of your submissions.");

        JButton viewDetailsButton = UiTheme.createSecondaryButton("View Job Details");
        JButton viewApplicationButton = UiTheme.createSecondaryButton("View Application Details");
        JButton withdrawButton = UiTheme.createSecondaryButton("Withdraw Application");
        JButton applyButton = UiTheme.createPrimaryButton("Apply");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh Tables");

        viewDetailsButton.addActionListener(event -> viewSelectedJob());
        viewApplicationButton.addActionListener(event -> viewSelectedApplication());
        withdrawButton.addActionListener(event -> withdrawSelectedApplication());
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
        body.add(UiTheme.createButtonRow(FlowLayout.LEFT, viewDetailsButton, viewApplicationButton, withdrawButton, applyButton, refreshButton), BorderLayout.NORTH);
        body.add(tabs, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Copies the current profile model into the visible form fields.
     */
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

    /**
     * Reads profile values from the form and persists them through the service layer.
     */
    private void saveProfile() {
        try {
            List<String> skillErrors = validationService.validateSkillInput(skillsField.getText(), "Skills", false);
            if (!skillErrors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\n", skillErrors));
            }
            // Read the latest UI state into the cached profile object before validation.
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

    /**
     * Reloads open jobs into the job table.
     */
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

    /**
     * Reloads this TA's applications into the application table.
     */
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

    /**
     * Opens the details dialog for the currently selected job.
     */
    private void viewSelectedJob() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select a job from the Available Jobs table before viewing details.");
            return;
        }
        String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
        JobPosting job = findJob(jobId).orElse(null);
        if (job == null) {
            UiMessage.error(this, "This job is no longer available. Please refresh the table.");
            refreshJobs();
            return;
        }
        String taDemandSummary = buildTaDemandText(job);
        new JobDetailsDialog(this, job, taDemandSummary).setVisible(true);
    }

    /**
     * Shows a textual summary of the selected application and linked job.
     */
    private void viewSelectedApplication() {
        int row = applicationTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an application from My Applications before viewing details.");
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

        JobPosting job = findJob(application.getJobId()).orElse(null);
        // Keep the popup text compact but complete so a TA can understand the application outcome without
        // switching between the table, job details dialog, and profile panel.
        String details = "Application ID: " + application.getApplicationId() + "\n" +
                "Job: " + (job == null ? "[Deleted Job]" : job.getModuleCode() + " - " + job.getModuleTitle()) + "\n" +
                "Status: " + application.getStatus() + "\n" +
                "Applied At: " + valueOrDash(application.getAppliedAt() == null ? null : application.getAppliedAt().toString()) + "\n" +
                "Match Score: " + application.getMatchScore() + "%\n" +
                "Missing Skills: " + valueOrDash(String.join(", ", application.getMissingSkills())) + "\n" +
                "Reviewer Notes: " + valueOrDash(application.getReviewerNotes()) + "\n" +
                "TA Demand: " + (job == null ? "-" : buildTaDemandText(job)) + "\n" +
                "Deadline: " + valueOrDash(job == null || job.getApplicationDeadline() == null ? null : job.getApplicationDeadline().toString());
        UiMessage.info(this, details);
    }

    /**
     * Submits an application for the selected job.
     */
    private void applyForSelectedJob() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select a job from the Available Jobs table before applying.");
            return;
        }
        List<String> profileIssues = getProfileIssuesForApplication();
        if (!profileIssues.isEmpty()) {
            UiMessage.error(this,
                    "Please complete your profile before applying.\nMissing or invalid items:\n- "
                            + String.join("\n- ", profileIssues)
                            + "\n\nSave your profile first, then try again.");
            return;
        }

        try {
            String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
            JobPosting job = findJob(jobId)
                    .orElseThrow(() -> new IllegalStateException("This job is no longer available. Please refresh the table."));
            ApplicationRecord record = applicationService.apply(profile, job);
            UiMessage.info(this,
                    "Application submitted for " + job.getModuleCode() + " - " + job.getModuleTitle()
                            + ".\nMatch score: " + record.getMatchScore()
                            + "%\nYou can track updates in My Applications.");
            refreshApplications();
            refreshJobs();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Withdraws the selected application after confirmation.
     */
    private void withdrawSelectedApplication() {
        int row = applicationTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an application from My Applications before withdrawing it.");
            return;
        }
        String applicationId = String.valueOf(applicationTableModel.getValueAt(row, 0));
        if (!UiMessage.confirm(this, "Are you sure you want to withdraw this application?", "Confirm Withdrawal")) {
            return;
        }

        try {
            applicationService.withdrawApplication(applicationId);
            UiMessage.info(this, "Application withdrawn.");
            refreshApplications();
            refreshJobs();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Returns to the login frame and closes the dashboard.
     */
    private void returnToLogin() {
        new LoginFrame(dataService).setVisible(true);
        dispose();
    }

    /**
     * Deletes the current TA account together with its profile and applications.
     */
    private void deleteCurrentAccount() {
        String confirmationMessage = "Are you sure you want to delete your TA account?\n"
                + "This will permanently remove your profile and all of your applications.";
        if (!UiMessage.confirm(this, confirmationMessage, "Delete Account")) {
            return;
        }

        try {
            applicationService.removeApplicationsForApplicant(profile.getApplicantId());

            List<ApplicantProfile> profiles = new ArrayList<>(dataService.getProfileRepository().findAll());
            profiles.removeIf(existingProfile -> profile.getApplicantId().equals(existingProfile.getApplicantId()));
            dataService.getProfileRepository().saveAll(profiles);

            List<User> users = new ArrayList<>(dataService.getUserRepository().findAll());
            users.removeIf(user -> currentUser.getUserId().equals(user.getUserId()));
            dataService.getUserRepository().saveAll(users);

            returnToLogin();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Builds the read-only CV path field plus choose-file button.
     */
    private JPanel buildCvPathPicker() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.add(cvPathField, BorderLayout.CENTER);
        panel.add(chooseCvButton, BorderLayout.EAST);
        return panel;
    }

    /**
     * Opens a file chooser and stores the selected CV path in the form.
     */
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

    /**
     * Applies the shared visual styling and custom renderers used by this frame.
     */
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
        jobTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        applicationTable.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer());
        UiTheme.setColumnWidths(jobTable, 90, 280, 80, 100, 130, 220, 100);
        UiTheme.setColumnWidths(applicationTable, 120, 90, 120, 90, 220, 280);
    }

    /**
     * Returns a dash for null or blank text to keep UI output readable.
     */
    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * Formats current TA demand as accepted/required.
     */
    private String buildTaDemandText(JobPosting job) {
        // TA demand is presented as accepted/required so the applicant sees remaining competition at a glance.
        int acceptedCount = applicationService.getAcceptedCountForJob(job.getJobId());
        return Math.min(acceptedCount, job.getRequiredTaCount()) + "/" + job.getRequiredTaCount();
    }

    private List<String> getProfileIssuesForApplication() {
        List<String> issues = new ArrayList<>();
        if (profile == null) {
            issues.add("Applicant profile could not be loaded");
            return issues;
        }

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String cvPath = cvPathField.getText().trim();

        if (name.isBlank()) {
            issues.add("Name");
        }
        if (email.isBlank()) {
            issues.add("Email");
        }
        if (phone.isBlank()) {
            issues.add("Phone number");
        }
        if (cvPath.isBlank()) {
            issues.add("CV file");
        }

        List<String> profileErrors = validationService.validateApplicantProfile(name, email, phone);
        for (String error : profileErrors) {
            if ("Name is required.".equals(error)
                    || "A valid email is required.".equals(error) && email.isBlank()
                    || "Phone number is required.".equals(error)) {
                continue;
            }
            issues.add(error);
        }
        issues.addAll(validationService.validateCvPath(cvPath));
        return issues;
    }
    /**
     * Wraps a text area in a themed scroll pane.
     */
    private JScrollPane wrapArea(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
    }

    /**
     * Reads a job by id without throwing so UI actions can degrade gracefully if
     * data changed outside the current screen.
     */
    private Optional<JobPosting> findJob(String jobId) {
        return dataService.getJobRepository().findById(jobId);
    }

    /**
     * Renderer that highlights rows with approaching or expired deadlines.
     */
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

    /**
     * Renderer that turns status values into colored badges.
     */
    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (isSelected) {
                return label;
            }

            String status = value == null ? "" : value.toString();
            label.setForeground(new Color(28, 37, 54));
            switch (status) {
                case "SUBMITTED" -> label.setBackground(new Color(232, 240, 255));
                case "SHORTLISTED" -> label.setBackground(new Color(255, 245, 204));
                case "ACCEPTED" -> label.setBackground(new Color(222, 245, 229));
                case "REJECTED" -> label.setBackground(new Color(255, 224, 224));
                case "WITHDRAWN" -> label.setBackground(new Color(234, 234, 234));
                case "OPEN" -> label.setBackground(new Color(232, 240, 255));
                case "CLOSED" -> label.setBackground(new Color(234, 234, 234));
                default -> label.setBackground(Color.WHITE);
            }
            return label;
        }
    }
}
