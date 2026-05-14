package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.JobCategory;
import model.JobPosting;
import model.NotificationRecord;
import model.User;
import service.ApplicantService;
import service.ApplicationService;
import service.CvStorageService;
import service.DataService;
import service.JobService;
import service.MatchingService;
import service.NotificationService;
import service.ValidationService;
import ui.dialogs.JobDetailsDialog;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final NotificationService notificationService;
    /**
     * Validation helper for parsing form input.
     */
    private final ValidationService validationService;
    private final CvStorageService cvStorageService;
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
    private final JTextField programmeField = new JTextField();
    private final JTextField yearOfStudyField = new JTextField();
    private final JTextField skillsField = new JTextField();
    private final JTextField availabilityField = new JTextField();
    private final JTextField preferredDutiesField = new JTextField();
    private final JTextArea experienceArea = new JTextArea(3, 20);
    private final JTextField cvPathField = new JTextField();
    private final JTextField supportingDocumentPathField = new JTextField();
    private final JButton chooseCvButton = UiTheme.createSecondaryButton("Choose File");
    private final JButton chooseSupportingDocumentButton = UiTheme.createSecondaryButton("Choose File");
    private final JTextField jobSearchField = new JTextField();
    private final JComboBox<String> moduleFilterBox = new JComboBox<>();
    private final JComboBox<String> categoryFilterBox = new JComboBox<>();
    private boolean syncingJobFilters;
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Category", "Semester", "Hours", "TA Demand", "Deadline", "Skills", "Favourite", "Status"}, 0);
    private final JTable jobTable = new PlaceholderTable(jobTableModel, "No open jobs are available right now.");
    private final DefaultTableModel favoriteJobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Category", "Semester", "Hours", "TA Demand", "Deadline", "Skills", "Favourite", "Status"}, 0);
    private final JTable favoriteJobTable = new PlaceholderTable(favoriteJobTableModel, "No favourite jobs match the current filters.");
    private final DefaultTableModel applicationTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Job ID", "Status", "Match %", "Missing Skills", "Reviewer Notes"}, 0);
    private final JTable applicationTable = new PlaceholderTable(applicationTableModel, "You have not submitted any applications yet.");
    private final DefaultTableModel notificationTableModel = new DefaultTableModel(
            new Object[]{"Time", "Status", "Message"}, 0);
    private final JTable notificationTable = new PlaceholderTable(notificationTableModel, "No notifications yet.");
    private final JTabbedPane opportunityTabs = new JTabbedPane();

    /**
     * Constructs the TA dashboard and loads the initial data snapshot.
     */
    public TADashboardFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;
        this.validationService = new ValidationService();
        this.cvStorageService = new CvStorageService();
        this.applicantService = new ApplicantService(dataService.getProfileRepository(), validationService);
        this.jobService = new JobService(dataService.getJobRepository(), validationService);
        this.applicationService = new ApplicationService(
                dataService.getApplicationRepository(),
                dataService.getJobRepository(),
                new MatchingService(),
                new service.AllocationService(dataService.getAllocationRepository())
        );
        this.notificationService = new NotificationService(dataService.getNotificationRepository());
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
        refreshNotifications();
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
        UiTheme.addFormRow(form, 6, "Programme", programmeField);
        UiTheme.addFormRow(form, 8, "Year of Study", yearOfStudyField);
        UiTheme.addFormRow(form, 10, "Skills", skillsField);
        UiTheme.addFormRow(form, 12, "Availability", availabilityField);
        UiTheme.addFormRow(form, 14, "Preferred Duties", preferredDutiesField);
        UiTheme.addFormRow(form, 16, "Experience Summary", wrapArea(experienceArea));
        UiTheme.addFormRow(form, 18, "CV Path", buildCvPathPicker());
        UiTheme.addFormRow(form, 20, "Supporting Document", buildSupportingDocumentPicker());

        JButton saveProfileButton = UiTheme.createPrimaryButton("Save Profile");
        JButton deleteAccountButton = UiTheme.createDangerButton("Delete Account");
        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");

        deleteAccountButton.addActionListener(event -> deleteCurrentAccount());
        backButton.addActionListener(event -> returnToLogin());
        saveProfileButton.addActionListener(event -> saveProfile());
        chooseCvButton.addActionListener(event -> chooseCvFile());
        chooseSupportingDocumentButton.addActionListener(event -> chooseSupportingDocumentFile());
        refreshButton.addActionListener(event -> {
            profile = applicantService.getProfileByUserId(currentUser.getUserId());
            loadProfile();
            refreshJobs();
            refreshApplications();
            refreshNotifications();
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
        JButton favouriteButton = UiTheme.createSecondaryButton("Toggle Favourite");
        JButton applyButton = UiTheme.createPrimaryButton("Apply");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh Tables");
        JButton markReadButton = UiTheme.createSecondaryButton("Mark Notifications Read");

        viewDetailsButton.addActionListener(event -> viewSelectedJob());
        viewApplicationButton.addActionListener(event -> viewSelectedApplication());
        withdrawButton.addActionListener(event -> withdrawSelectedApplication());
        favouriteButton.addActionListener(event -> toggleSelectedFavouriteJob());
        applyButton.addActionListener(event -> applyForSelectedJob());
        markReadButton.addActionListener(event -> {
            notificationService.markAllRead(currentUser.getUserId());
            refreshNotifications();
        });
        refreshButton.addActionListener(event -> {
            refreshJobs();
            refreshApplications();
            refreshNotifications();
        });

        UiTheme.styleTabs(opportunityTabs);
        opportunityTabs.addTab("Available Jobs", UiTheme.wrapTable(jobTable));
        opportunityTabs.addTab("Favourite Jobs", UiTheme.wrapTable(favoriteJobTable));
        opportunityTabs.addTab("My Applications", UiTheme.wrapTable(applicationTable));
        opportunityTabs.addTab("Notifications", UiTheme.wrapTable(notificationTable));

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        JPanel controls = new JPanel(new BorderLayout(0, 10));
        controls.setOpaque(false);
        controls.add(buildJobFilterPanel(), BorderLayout.NORTH);
        controls.add(UiTheme.createButtonRow(FlowLayout.LEFT, viewDetailsButton, viewApplicationButton, withdrawButton, favouriteButton, applyButton, markReadButton, refreshButton), BorderLayout.SOUTH);
        body.add(controls, BorderLayout.NORTH);
        body.add(opportunityTabs, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildJobFilterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));
        panel.setOpaque(false);

        UiTheme.styleTextField(jobSearchField);
        UiTheme.styleComboBox(moduleFilterBox);
        UiTheme.styleComboBox(categoryFilterBox);

        jobSearchField.setToolTipText("Search module, duties, semester, category, or required skills.");
        moduleFilterBox.setToolTipText("Filter vacancies by module.");
        categoryFilterBox.setToolTipText("Filter vacancies by activity type.");

        jobSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshJobs();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshJobs();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshJobs();
            }
        });
        moduleFilterBox.addActionListener(event -> {
            if (!syncingJobFilters) {
                refreshJobs();
            }
        });
        categoryFilterBox.addActionListener(event -> {
            if (!syncingJobFilters) {
                refreshJobs();
            }
        });

        panel.add(labeledControl("Search", jobSearchField));
        panel.add(labeledControl("Module", moduleFilterBox));
        panel.add(labeledControl("Category", categoryFilterBox));
        return panel;
    }

    private JPanel labeledControl(String labelText, JComponent control) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.uiFont(Font.BOLD, 12));
        panel.add(label, BorderLayout.NORTH);
        panel.add(control, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Copies the current profile model into the visible form fields.
     */
    private void loadProfile() {
        nameField.setText(profile.getName());
        emailField.setText(profile.getEmail());
        phoneField.setText(profile.getPhone());
        programmeField.setText(profile.getProgramme());
        yearOfStudyField.setText(profile.getYearOfStudy());
        skillsField.setText(String.join(", ", profile.getSkills()));
        availabilityField.setText(profile.getAvailability());
        preferredDutiesField.setText(profile.getPreferredDuties());
        experienceArea.setText(profile.getExperienceSummary());
        cvPathField.setText(profile.getCvPath());
        updateCvPathOpenState();
        supportingDocumentPathField.setText(profile.getSupportingDocumentPath());
        updateSupportingDocumentOpenState();
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
            profile.setProgramme(programmeField.getText().trim());
            profile.setYearOfStudy(yearOfStudyField.getText().trim());
            profile.setSkills(validationService.parseSkills(skillsField.getText()));
            profile.setAvailability(availabilityField.getText().trim());
            profile.setPreferredDuties(preferredDutiesField.getText().trim());
            profile.setExperienceSummary(experienceArea.getText().trim());
            List<String> profileErrors = validationService.validateApplicantProfile(
                    profile.getName(),
                    profile.getEmail(),
                    profile.getPhone()
            );
            profileErrors.addAll(validationService.validateAcademicProfile(
                    profile.getProgramme(),
                    profile.getYearOfStudy()
            ));
            profileErrors.addAll(validationService.validateCvPath(cvPathField.getText().trim()));
            profileErrors.addAll(validationService.validateSupportingDocumentPath(supportingDocumentPathField.getText().trim()));
            if (!profileErrors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\n", profileErrors));
            }
            String storedCvPath = cvStorageService.storeCvForApplicant(
                    profile.getApplicantId(),
                    cvPathField.getText().trim(),
                    profile.getCvPath()
            );
            String storedSupportingDocumentPath = cvStorageService.storeSupportingDocumentForApplicant(
                    profile.getApplicantId(),
                    supportingDocumentPathField.getText().trim(),
                    profile.getSupportingDocumentPath()
            );
            profile.setCvPath(storedCvPath);
            profile.setSupportingDocumentPath(storedSupportingDocumentPath);
            applicantService.saveProfile(profile);
            cvPathField.setText(profile.getCvPath());
            supportingDocumentPathField.setText(profile.getSupportingDocumentPath());
            updateCvPathOpenState();
            updateSupportingDocumentOpenState();
            UiMessage.info(this, "Profile saved successfully.");
            refreshJobs();
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
        favoriteJobTableModel.setRowCount(0);
        refreshJobFilterOptions();

        String module = String.valueOf(moduleFilterBox.getSelectedItem());
        if ("All Modules".equals(module)) {
            module = "";
        }
        JobCategory category = selectedCategoryFilter();
        List<JobPosting> jobs = jobService.searchOpenJobs(jobSearchField.getText(), module, category).stream()
                .filter(job -> job.getApplicationDeadline() == null || !job.getApplicationDeadline().isBefore(LocalDate.now()))
                .toList();

        for (JobPosting job : jobs) {
            Object[] row = buildJobRow(job);
            jobTableModel.addRow(row);
            if (profile != null && profile.isFavoriteJob(job.getJobId())) {
                favoriteJobTableModel.addRow(row);
            }
        }
    }

    private void refreshJobFilterOptions() {
        syncingJobFilters = true;
        String selectedModule = String.valueOf(moduleFilterBox.getSelectedItem());
        String selectedCategory = String.valueOf(categoryFilterBox.getSelectedItem());

        moduleFilterBox.removeAllItems();
        moduleFilterBox.addItem("All Modules");
        jobService.getOpenJobs().stream()
                .map(JobPosting::getModuleCode)
                .distinct()
                .sorted()
                .forEach(moduleFilterBox::addItem);
        if (selectedModule != null && comboContains(moduleFilterBox, selectedModule)) {
            moduleFilterBox.setSelectedItem(selectedModule);
        }

        categoryFilterBox.removeAllItems();
        categoryFilterBox.addItem("All Categories");
        for (JobCategory category : JobCategory.values()) {
            categoryFilterBox.addItem(category.getDisplayName());
        }
        if (selectedCategory != null && comboContains(categoryFilterBox, selectedCategory)) {
            categoryFilterBox.setSelectedItem(selectedCategory);
        }
        syncingJobFilters = false;
    }

    private boolean comboContains(JComboBox<String> comboBox, String value) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (value.equals(comboBox.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    private JobCategory selectedCategoryFilter() {
        String selected = String.valueOf(categoryFilterBox.getSelectedItem());
        for (JobCategory category : JobCategory.values()) {
            if (category.getDisplayName().equals(selected)) {
                return category;
            }
        }
        return null;
    }

    private Object[] buildJobRow(JobPosting job) {
        return new Object[]{
                job.getJobId(),
                job.getModuleCode() + " - " + job.getModuleTitle(),
                job.getCategory() == null ? "-" : job.getCategory().getDisplayName(),
                valueOrDash(job.getSemester()),
                job.getHours(),
                buildTaDemandText(job),
                job.getApplicationDeadline(),
                String.join(", ", job.getRequiredSkills()),
                profile != null && profile.isFavoriteJob(job.getJobId()) ? "Yes" : "No",
                job.getStatus()
        };
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
        String jobId = selectedJobId();
        if (jobId == null) {
            UiMessage.error(this, "Please select a job from the Available Jobs table before viewing details.");
            return;
        }
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
                "Last Updated: " + valueOrDash(application.getLastUpdatedAt() == null ? null : application.getLastUpdatedAt().toString()) + "\n" +
                "Decision At: " + valueOrDash(application.getDecisionAt() == null ? null : application.getDecisionAt().toString()) + "\n" +
                "Match Score: " + application.getMatchScore() + "%\n" +
                "Missing Skills: " + valueOrDash(String.join(", ", application.getMissingSkills())) + "\n" +
                "Suggestion: " + buildMissingSkillSuggestion(application) + "\n" +
                "Reviewer Notes: " + valueOrDash(application.getReviewerNotes()) + "\n" +
                "TA Demand: " + (job == null ? "-" : buildTaDemandText(job)) + "\n" +
                "Deadline: " + valueOrDash(job == null || job.getApplicationDeadline() == null ? null : job.getApplicationDeadline().toString());
        UiMessage.info(this, details);
    }

    /**
     * Submits an application for the selected job.
     */
    private void applyForSelectedJob() {
        String jobId = selectedJobId();
        if (jobId == null) {
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
            JobPosting job = findJob(jobId)
                    .orElseThrow(() -> new IllegalStateException("This job is no longer available. Please refresh the table."));
            ApplicationRecord record = applicationService.apply(profile, job);
            notificationService.notifyUser(currentUser.getUserId(), "Application submitted for " + job.getModuleCode() + " " + job.getModuleTitle() + ".");
            notificationService.notifyUser(job.getPostedBy(), profile.getName() + " submitted an application for " + job.getModuleCode() + " " + job.getModuleTitle() + ".");
            UiMessage.info(this,
                    "Application submitted for " + job.getModuleCode() + " - " + job.getModuleTitle()
                            + ".\nMatch score: " + record.getMatchScore()
                            + "%\nYou can track updates in My Applications.");
            refreshApplications();
            refreshJobs();
            refreshNotifications();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    private void refreshNotifications() {
        notificationTableModel.setRowCount(0);
        for (NotificationRecord notification : notificationService.getNotificationsForUser(currentUser.getUserId())) {
            notificationTableModel.addRow(new Object[]{
                    notification.getCreatedAt() == null ? "-" : notification.getCreatedAt(),
                    notification.isRead() ? "Read" : "New",
                    notification.getMessage()
            });
        }
    }

    private void toggleSelectedFavouriteJob() {
        String jobId = selectedJobId();
        if (jobId == null) {
            UiMessage.error(this, "Please select a job before changing favourites.");
            return;
        }
        List<String> favourites = new ArrayList<>(profile.getFavoriteJobIds());
        if (favourites.contains(jobId)) {
            favourites.remove(jobId);
            UiMessage.info(this, "Job removed from favourites.");
        } else {
            favourites.add(jobId);
            UiMessage.info(this, "Job saved to favourites.");
        }
        profile.setFavoriteJobIds(favourites);
        saveProfileRecordWithoutValidation();
        refreshJobs();
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
            ApplicationRecord selectedApplication = applicationService.getApplicationsForApplicant(profile.getApplicantId()).stream()
                    .filter(record -> applicationId.equals(record.getApplicationId()))
                    .findFirst()
                    .orElse(null);
            applicationService.withdrawApplication(applicationId, currentUser.getUserId());
            if (selectedApplication != null) {
                JobPosting job = findJob(selectedApplication.getJobId()).orElse(null);
                if (job != null) {
                    notificationService.notifyUser(currentUser.getUserId(), "Application withdrawn for " + job.getModuleCode() + " " + job.getModuleTitle() + ".");
                    notificationService.notifyUser(job.getPostedBy(), profile.getName() + " withdrew an application for " + job.getModuleCode() + " " + job.getModuleTitle() + ".");
                }
            }
            UiMessage.info(this, "Application withdrawn.");
            refreshApplications();
            refreshJobs();
            refreshNotifications();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    private String selectedJobId() {
        int selectedTab = opportunityTabs.getSelectedIndex();
        if (selectedTab != 0 && selectedTab != 1) {
            return null;
        }
        JTable selectedTable = selectedTab == 1 ? favoriteJobTable : jobTable;
        DefaultTableModel model = selectedTable == favoriteJobTable ? favoriteJobTableModel : jobTableModel;
        int row = selectedTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return String.valueOf(model.getValueAt(row, 0));
    }

    private void saveProfileRecordWithoutValidation() {
        List<ApplicantProfile> profiles = new ArrayList<>(dataService.getProfileRepository().findAll());
        for (int i = 0; i < profiles.size(); i++) {
            if (profile.getApplicantId().equals(profiles.get(i).getApplicantId())) {
                profiles.set(i, profile);
                dataService.getProfileRepository().saveAll(profiles);
                return;
            }
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
            cvStorageService.deleteManagedCv(profile.getCvPath());

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

    private JPanel buildSupportingDocumentPicker() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.add(supportingDocumentPathField, BorderLayout.CENTER);
        panel.add(chooseSupportingDocumentButton, BorderLayout.EAST);
        return panel;
    }

    /**
     * Opens a file chooser and stores the selected CV path in the form.
     */
    private void chooseCvFile() {
        chooseDocumentFile(cvPathField, true);
    }

    private void chooseSupportingDocumentFile() {
        chooseDocumentFile(supportingDocumentPathField, false);
    }

    private void chooseDocumentFile(JTextField targetField, boolean cvFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(cvFile ? "Select CV File" : "Select Supporting Document");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
                cvFile ? CV_FILE_DESCRIPTION : "Supporting Documents (*.pdf, *.doc, *.docx, *.rtf, *.txt)",
                "pdf", "doc", "docx", "rtf", "txt"
        ));
        UiTheme.styleFileChooser(chooser);

        String existingPath = targetField.getText().trim();
        if (!existingPath.isBlank()) {
            File existingFile = cvStorageService.resolveCvPath(existingPath).toFile();
            if (existingFile.exists()) {
                chooser.setSelectedFile(existingFile);
            }
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            File selectedFile = chooser.getSelectedFile();
            String selectedPath = selectedFile.getAbsolutePath();
            List<String> errors = cvFile
                    ? validationService.validateCvPath(selectedPath)
                    : validationService.validateSupportingDocumentPath(selectedPath);
            if (!errors.isEmpty()) {
                UiMessage.error(this, String.join("\n", errors));
                return;
            }
            targetField.setText(selectedPath);
            if (cvFile) {
                updateCvPathOpenState();
            } else {
                updateSupportingDocumentOpenState();
            }
        }
    }

    private void updateCvPathOpenState() {
        String cvPath = cvPathField.getText().trim();
        boolean hasCvPath = !cvPath.isBlank();
        cvPathField.setCursor(Cursor.getPredefinedCursor(hasCvPath ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        cvPathField.setToolTipText(hasCvPath ? "Click to open " + cvPath : "Choose your CV file to fill this path automatically.");
    }

    private void updateSupportingDocumentOpenState() {
        String documentPath = supportingDocumentPathField.getText().trim();
        boolean hasDocument = !documentPath.isBlank();
        supportingDocumentPathField.setCursor(Cursor.getPredefinedCursor(hasDocument ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        supportingDocumentPathField.setToolTipText(hasDocument ? "Click to open " + documentPath : "Optionally choose a transcript or supporting document.");
    }

    private void openCvFile() {
        openDocumentFile(cvPathField.getText().trim(), "CV file");
    }

    private void openSupportingDocumentFile() {
        openDocumentFile(supportingDocumentPathField.getText().trim(), "supporting document");
    }

    private void openDocumentFile(String documentPath, String label) {
        if (documentPath.isBlank()) {
            UiMessage.error(this, "No " + label + " is available. Please choose a file first.");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            UiMessage.error(this, "This computer does not support opening files from the application.");
            return;
        }

        File cvFile = cvStorageService.resolveCvPath(documentPath).toFile();
        if (!cvFile.isFile()) {
            UiMessage.error(this, "The " + label + " could not be found:\n" + documentPath);
            return;
        }

        try {
            Desktop.getDesktop().open(cvFile);
        } catch (Exception ex) {
            UiMessage.error(this, "Could not open the " + label + ":\n" + ex.getMessage());
        }
    }

    /**
     * Applies the shared visual styling and custom renderers used by this frame.
     */
    private void styleComponents() {
        UiTheme.styleTextField(nameField);
        UiTheme.styleTextField(emailField);
        UiTheme.styleTextField(phoneField);
        UiTheme.styleTextField(programmeField);
        UiTheme.styleTextField(yearOfStudyField);
        UiTheme.styleTextField(skillsField);
        UiTheme.styleTextField(availabilityField);
        UiTheme.styleTextField(preferredDutiesField);
        UiTheme.styleTextField(cvPathField);
        UiTheme.styleTextField(supportingDocumentPathField);
        cvPathField.setEditable(false);
        cvPathField.setToolTipText("Choose your CV file to fill this path automatically.");
        updateCvPathOpenState();
        cvPathField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!cvPathField.getText().trim().isBlank()) {
                    openCvFile();
                }
            }
        });
        supportingDocumentPathField.setEditable(false);
        supportingDocumentPathField.setToolTipText("Optionally choose a transcript or supporting document.");
        updateSupportingDocumentOpenState();
        supportingDocumentPathField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!supportingDocumentPathField.getText().trim().isBlank()) {
                    openSupportingDocumentFile();
                }
            }
        });
        UiTheme.styleTextArea(experienceArea, 5);
        UiTheme.styleTable(jobTable);
        UiTheme.styleTable(favoriteJobTable);
        UiTheme.styleTable(applicationTable);
        UiTheme.styleTable(notificationTable);
        jobTable.getColumnModel().getColumn(6).setCellRenderer(new DeadlineWarningRenderer());
        jobTable.getColumnModel().getColumn(9).setCellRenderer(new StatusBadgeRenderer());
        favoriteJobTable.getColumnModel().getColumn(6).setCellRenderer(new DeadlineWarningRenderer());
        favoriteJobTable.getColumnModel().getColumn(9).setCellRenderer(new StatusBadgeRenderer());
        applicationTable.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer());
        UiTheme.setColumnWidths(jobTable, 90, 260, 130, 110, 70, 100, 120, 220, 90, 90);
        UiTheme.setColumnWidths(favoriteJobTable, 90, 260, 130, 110, 70, 100, 120, 220, 90, 90);
        UiTheme.setColumnWidths(applicationTable, 120, 90, 120, 90, 220, 280);
        UiTheme.setColumnWidths(notificationTable, 160, 80, 560);
    }

    /**
     * Returns a dash for null or blank text to keep UI output readable.
     */
    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String buildMissingSkillSuggestion(ApplicationRecord application) {
        if (application.getMissingSkills().isEmpty()) {
            return "No missing skills were identified for this application.";
        }
        return "Consider strengthening or documenting: " + String.join(", ", application.getMissingSkills()) + ".";
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
        String programme = programmeField.getText().trim();
        String yearOfStudy = yearOfStudyField.getText().trim();
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
        if (programme.isBlank()) {
            issues.add("Programme");
        }
        if (yearOfStudy.isBlank()) {
            issues.add("Year of study");
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
        issues.addAll(validationService.validateAcademicProfile(programme, yearOfStudy));
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
