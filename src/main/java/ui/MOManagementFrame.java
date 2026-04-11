package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.JobPosting;
import model.JobStatus;
import model.Role;
import model.SkillMatchResult;
import model.User;
import service.ApplicantService;
import service.ApplicationService;
import service.DataService;
import service.JobService;
import service.MatchingService;
import service.ValidationService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MO dashboard for posting jobs and reviewing applicants.
 *
 * This frame is shared by normal Module Organisers and admins. Normal MOs only
 * see their assigned modules, while admins can work across all known modules.
 * It combines three responsibilities in one screen: editing job postings,
 * loading applications for a selected job, and recording review decisions.
 */
public class MOManagementFrame extends JFrame {
    // New jobs do not have a repository ID yet, so the form shows this until save generates one.
    private static final String NEW_JOB_PLACEHOLDER = "AUTO-GENERATED ON SAVE";

    // Services are kept as fields because most button actions need to read or save data.
    private final DataService dataService;
    private final JobService jobService;
    private final ApplicantService applicantService;
    private final ApplicationService applicationService;
    private final MatchingService matchingService;
    private final ValidationService validationService;
    private final User currentUser;
    private final List<String> managedModuleCodes;
    private final boolean adminMode;
    // Prevents combo box/list listeners from reloading data while the form is being filled programmatically.
    private boolean syncingForm;

    // Left-side job posting form fields.
    private final JTextField jobIdField = new JTextField();
    private final JComboBox<String> moduleCodeBox = new JComboBox<>();
    private final JTextField moduleTitleField = new JTextField();
    private final JTextField hoursField = new JTextField();
    private final JTextField requiredTaCountField = new JTextField();
    private final JTextField skillsField = new JTextField();
    private final JTextField deadlineField = new JTextField();
    private final JComboBox<JobStatus> statusBox = new JComboBox<>(JobStatus.values());
    private final JTextArea dutiesArea = new JTextArea(3, 20);
    private final JTextArea reviewArea = new JTextArea(4, 20);
    // Read-only text areas that show details for the selected applicant/application.
    private final JTextArea applicantSummaryArea = new JTextArea(4, 20);
    private final JTextArea matchInfoArea = new JTextArea(6, 20);
    // Filter and sort controls are reapplied each time the applicant table reloads.
    private final JComboBox<String> applicantStatusFilter = new JComboBox<>(
            new String[]{"All Statuses", "SUBMITTED", "SHORTLISTED", "ACCEPTED", "REJECTED", "WITHDRAWN"}
    );
    private final JComboBox<String> applicantSortBox = new JComboBox<>(
            new String[]{"Match % (High to Low)", "Match % (Low to High)", "Applicant Name (A-Z)", "Applicant Name (Z-A)", "Status"}
    );
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Hours", "TA Demand", "Deadline", "Status"}, 0);
    private final PlaceholderTable jobTable = new PlaceholderTable(jobTableModel, "No jobs are assigned to this MO yet.");
    private final DefaultTableModel applicantTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Applicant", "Status", "Match %", "Missing"}, 0);
    private final PlaceholderTable applicantTable = new PlaceholderTable(applicantTableModel, "Select a job and click Load Applicants to review submissions.");
    // Keeps the applicant table tied to the job currently loaded after filters or sorting change.
    private String loadedApplicantJobId;

    public MOManagementFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;
        this.validationService = new ValidationService();
        // Each service receives the repository it owns; the frame stays focused on UI coordination.
        this.jobService = new JobService(dataService.getJobRepository(), validationService);
        this.applicantService = new ApplicantService(dataService.getProfileRepository(), validationService);
        this.matchingService = new MatchingService();
        this.applicationService = new ApplicationService(
                dataService.getApplicationRepository(),
                dataService.getJobRepository(),
                matchingService
        );
        this.adminMode = currentUser.getRole() == Role.ADMIN;
        // The module list is resolved once when the frame opens, then used for filtering and validation.
        this.managedModuleCodes = resolveManagedModuleCodes();

        // Build the main two-column layout: job editor on the left, review queue on the right.
        setTitle((adminMode ? "Hiring Management" : "MO Management") + " - " + Constants.APP_TITLE);
        setSize(1420, 880);
        setMinimumSize(new Dimension(1020, 700));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);
        styleComponents();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildFormPanel(), buildTablesPanel());
        splitPane.setResizeWeight(0.4);
        UiTheme.styleSplitPane(splitPane);

        JPanel root = UiTheme.createPagePanel();
        root.add(UiTheme.createHeader(
                adminMode ? "Admin Hiring Console" : "Module Organiser Console",
                buildScopeSummary()
        ), BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        add(UiTheme.wrapPage(root));

        // Initial load: show existing jobs first, then reset the editor into "new job" mode.
        refreshJobs();
        clearForm();
        bindFormSync();
    }

    /**
     * Creates the job editor form, including controls for creating a new posting
     * and updating an existing one.
     */
    private JPanel buildFormPanel() {
        JPanel panel = UiTheme.createCard("Job Posting Editor", "Select an existing job to revise it, or start a new vacancy from a clean form.");

        JPanel form = UiTheme.createFormGrid();
        // Job IDs are owned by JobService, so users can view them but not edit them directly.
        jobIdField.setEditable(false);
        jobIdField.setForeground(Color.GRAY);
        matchInfoArea.setEditable(false);
        populateManagedModules();
        UiTheme.addFormRow(form, 0, "Job ID", jobIdField);
        UiTheme.addFormRow(form, 2, "Module Code", moduleCodeBox);
        UiTheme.addFormRow(form, 4, "Module Title", moduleTitleField);
        UiTheme.addFormRow(form, 6, "Hours", hoursField);
        UiTheme.addFormRow(form, 8, "TA Needed", requiredTaCountField);
        UiTheme.addFormRow(form, 10, "Required Skills", skillsField);
        UiTheme.addFormRow(form, 12, "Deadline (YYYY-MM-DD)", deadlineField);
        UiTheme.addFormRow(form, 14, "Status", statusBox);
        UiTheme.addFormRow(form, 16, "Duties", wrapArea(dutiesArea));

        JPanel lower = UiTheme.createCard("Applicant Match Details", "Review fit, missing skills, and applicant notes for the selected submission.");
        lower.add(wrapArea(matchInfoArea), BorderLayout.CENTER);

        JButton backButton = adminMode
                ? UiTheme.createDangerButton("Back to Previous Page")
                : UiTheme.createSecondaryButton("Back to Login");
        JButton newButton = UiTheme.createSecondaryButton("New Job");
        JButton saveButton = UiTheme.createPrimaryButton("Save Job");

        // If the user has no modules, the form stays visible but cannot create invalid jobs.
        boolean hasManagedModules = !managedModuleCodes.isEmpty();
        newButton.setEnabled(hasManagedModules);
        saveButton.setEnabled(hasManagedModules);
        moduleCodeBox.setEnabled(hasManagedModules);

        // Button handlers are small and delegate the real work to helper methods below.
        backButton.addActionListener(event -> goBack());
        newButton.addActionListener(event -> clearForm());
        saveButton.addActionListener(event -> saveJob());

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UiTheme.wrapPage(form), BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, backButton, newButton, saveButton), BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the review area with the job table, applicant table, filters, and
     * status-change buttons used during applicant review.
     */
    private JPanel buildTablesPanel() {
        JPanel panel = UiTheme.createCard("Review Queue", "Inspect job postings on the top table and applicant submissions below.");

        JButton loadApplicantsButton = UiTheme.createSecondaryButton("Load Applicants");
        JButton shortlistButton = UiTheme.createSecondaryButton("Shortlist");
        JButton acceptButton = UiTheme.createPrimaryButton("Accept");
        JButton cancelAcceptanceButton = UiTheme.createSecondaryButton("Cancel Acceptance");
        JButton rejectButton = UiTheme.createDangerButton("Reject");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");

        // Review buttons operate on the currently selected applicant row.
        loadApplicantsButton.addActionListener(event -> loadApplicantsForSelectedJob());
        shortlistButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.SHORTLISTED));
        acceptButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.ACCEPTED));
        cancelAcceptanceButton.addActionListener(event -> cancelAcceptedApplication());
        rejectButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.REJECTED));
        refreshButton.addActionListener(event -> {
            refreshJobs();
            applicantTableModel.setRowCount(0);
            reviewArea.setText("");
            applicantSummaryArea.setText("");
            matchInfoArea.setText("");
            loadedApplicantJobId = null;
            updateApplicantEmptyState();
        });
        // Changing a filter or sort option reuses the last loaded job instead of requiring a new selection.
        applicantStatusFilter.addActionListener(event -> {
            if (!syncingForm && loadedApplicantJobId != null) {
                loadApplicantsForJob(loadedApplicantJobId);
            }
        });
        applicantSortBox.addActionListener(event -> {
            if (!syncingForm && loadedApplicantJobId != null) {
                loadApplicantsForJob(loadedApplicantJobId);
            }
        });

        // Selecting a job fills the editor; selecting an applicant fills the match and notes panels.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UiTheme.wrapTable(jobTable), UiTheme.wrapTable(applicantTable));
        splitPane.setResizeWeight(0.5);
        UiTheme.styleSplitPane(splitPane);
        jobTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedJobToForm();
            }
        });
        applicantTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedApplicantMatch();
            }
        });

        JLabel filterLabel = new JLabel("Applicant Filter");
        filterLabel.setForeground(UiTheme.TEXT);
        filterLabel.setFont(UiTheme.uiFont(Font.BOLD, 13));
        JLabel sortLabel = new JLabel("Sort");
        sortLabel.setForeground(UiTheme.TEXT);
        sortLabel.setFont(UiTheme.uiFont(Font.BOLD, 13));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(sortLabel);
        filterPanel.add(applicantSortBox);
        filterPanel.add(filterLabel);
        filterPanel.add(applicantStatusFilter);

        JPanel topControls = new JPanel(new BorderLayout(12, 0));
        topControls.setOpaque(false);
        topControls.add(UiTheme.createButtonRow(FlowLayout.LEFT, loadApplicantsButton, shortlistButton, acceptButton, cancelAcceptanceButton, rejectButton, refreshButton), BorderLayout.WEST);
        topControls.add(filterPanel, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(topControls, BorderLayout.NORTH);
        body.add(splitPane, BorderLayout.CENTER);
        body.add(buildReviewBottomPanel(), BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Reloads the job table using only jobs inside the current user's module scope.
     */
    private void refreshJobs() {
        jobTableModel.setRowCount(0);
        for (JobPosting job : getScopedJobs()) {
            // The table stores only display columns; full JobPosting objects are fetched by ID when needed.
            jobTableModel.addRow(new Object[]{
                    job.getJobId(),
                    job.getModuleCode() + " - " + job.getModuleTitle(),
                    job.getHours(),
                    buildTaDemandText(job),
                    job.getApplicationDeadline(),
                    job.getStatus()
            });
        }
    }

    /**
     * Copies the selected job from the table into the editor form.
     */
    private void loadSelectedJobToForm() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        // The table row may be stale if another action changed the repository, so the job is looked up again.
        String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
        JobPosting job = findJob(jobId).orElse(null);
        if (job == null) {
            UiMessage.error(this, "The selected job no longer exists. Refreshing the table now.");
            refreshJobs();
            clearForm();
            return;
        }
        applyJobToForm(job);
    }

    /**
     * Resets the form for creating a new job and clears applicant-specific panels.
     */
    private void clearForm() {
        syncingForm = true;
        // Clearing selections can fire listeners, so syncingForm stays true until the reset is complete.
        jobTable.clearSelection();
        applicantTable.clearSelection();
        jobIdField.setForeground(Color.GRAY);
        jobIdField.setText(NEW_JOB_PLACEHOLDER);
        if (moduleCodeBox.getItemCount() > 0) {
            moduleCodeBox.setSelectedIndex(0);
        } else {
            moduleCodeBox.setSelectedItem(null);
        }
        moduleTitleField.setText("");
        hoursField.setText("");
        requiredTaCountField.setText("1");
        skillsField.setText("");
        deadlineField.setText("");
        dutiesArea.setText("");
        statusBox.setSelectedItem(JobStatus.OPEN);
        matchInfoArea.setText("");
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        applicantTableModel.setRowCount(0);
        loadedApplicantJobId = null;
        updateApplicantEmptyState();
        moduleCodeBox.requestFocusInWindow();
        syncingForm = false;
    }

    /**
     * Validates form input, creates or updates a JobPosting, and handles the
     * extra applicant decisions needed when closing or reopening a job.
     */
    private void saveJob() {
        try {
            if (managedModuleCodes.isEmpty()) {
                UiMessage.error(this, adminMode
                        ? "No manageable module codes are available yet."
                        : "This MO is not assigned to any modules yet.");
                return;
            }
            String jobId = jobIdField.getText().trim();
            // A placeholder means this is a new posting; otherwise the existing job is loaded for comparison.
            JobPosting existingJob = NEW_JOB_PLACEHOLDER.equals(jobId) || jobId.isBlank()
                    ? null
                    : jobService.getJobById(jobId);
            String moduleCode = String.valueOf(moduleCodeBox.getSelectedItem()).trim();
            // Permission is checked both for the chosen module and the existing job being edited.
            if (!canManageModule(moduleCode)) {
                throw new IllegalArgumentException("You can only manage TA hiring for your assigned modules.");
            }
            if (existingJob != null && !canManageModule(existingJob.getModuleCode())) {
                throw new IllegalArgumentException("You can only edit jobs for your assigned modules.");
            }
            // Required skills are validated before parsing so the user receives one clear message.
            List<String> skillErrors = validationService.validateSkillInput(skillsField.getText(), "Required skills", true);
            if (!skillErrors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\n", skillErrors));
            }

            // Build a fresh domain object from the form so JobService validation receives a complete posting.
            JobPosting jobPosting = new JobPosting();
            jobPosting.setJobId(NEW_JOB_PLACEHOLDER.equals(jobId) ? "" : jobId);
            jobPosting.setModuleCode(moduleCode);
            jobPosting.setModuleTitle(moduleTitleField.getText().trim());
            jobPosting.setHours(Integer.parseInt(hoursField.getText().trim()));
            jobPosting.setRequiredTaCount(Integer.parseInt(requiredTaCountField.getText().trim()));
            jobPosting.setRequiredSkills(validationService.parseSkills(skillsField.getText()));
            jobPosting.setApplicationDeadline(LocalDate.parse(deadlineField.getText().trim()));
            jobPosting.setStatus((JobStatus) statusBox.getSelectedItem());
            jobPosting.setDuties(dutiesArea.getText().trim());
            jobPosting.setPostedBy(currentUser.getUserId());

            // Changing between OPEN and CLOSED can affect applicant statuses, so those paths are separated.
            if (jobPosting.getStatus() == JobStatus.CLOSED) {
                if (!handleClosingJob(jobPosting, existingJob)) {
                    return;
                }
            } else {
                if (!handleOpeningJob(jobPosting, existingJob)) {
                    return;
                }
            }
            UiMessage.info(this, "Job saved successfully.");
            // Reload from storage after saving so generated IDs and service-side updates are reflected.
            refreshJobs();
            clearForm();
        } catch (NumberFormatException ex) {
            UiMessage.error(this, "Hours and required TA count must be numbers.");
        } catch (DateTimeParseException ex) {
            UiMessage.error(this, "Deadline must use YYYY-MM-DD format, for example 2026-04-30.");
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Loads applicants for the job selected in the job table.
     */
    private void loadApplicantsForSelectedJob() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select a job first.");
            return;
        }
        String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
        loadApplicantsForJob(jobId);
    }

    /**
     * Displays the selected applicant's profile summary, reviewer notes, and
     * skill matching details for the currently selected job.
     */
    private void showSelectedApplicantMatch() {
        int applicationRow = applicantTable.getSelectedRow();
        int jobRow = jobTable.getSelectedRow();
        if (applicationRow < 0 || jobRow < 0) {
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(applicationRow, 0));
        String jobId = String.valueOf(jobTableModel.getValueAt(jobRow, 0));
        // Application rows only contain display data, so fetch the full record from the service.
        ApplicationRecord application = applicationService.getApplicationsForJob(jobId).stream()
                .filter(item -> item.getApplicationId().equals(applicationId))
                .findFirst()
                .orElse(null);
        if (application == null) {
            return;
        }
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        JobPosting job = findJob(jobId).orElse(null);
        if (applicant == null || job == null) {
            matchInfoArea.setText("The selected applicant or job record no longer exists. Refresh the review queue.");
            applicantSummaryArea.setText("");
            reviewArea.setText("");
            return;
        }
        // Match details are calculated live so the display reflects the latest job requirements.
        SkillMatchResult matchResult = matchingService.calculateMatch(applicant.getSkills(), job.getRequiredSkills());
        matchInfoArea.setText(
                "Applicant: " + applicant.getName() + "\n" +
                        "Skills: " + String.join(", ", applicant.getSkills()) + "\n" +
                        "CV: " + applicant.getCvPath() + "\n" +
                        "Score: " + matchResult.getScorePercentage() + "%\n" +
                        "Matched: " + String.join(", ", matchResult.getMatchedSkills()) + "\n" +
                        "Missing: " + String.join(", ", matchResult.getMissingSkills()) + "\n" +
                        matchResult.getExplanation()
        );
        applicantSummaryArea.setText(buildApplicantSummary(applicant, application, job));
        reviewArea.setText(application.getReviewerNotes() == null ? "" : application.getReviewerNotes());
    }

    /**
     * Applies a review status to the selected application and refreshes the
     * same job afterwards so the reviewer does not lose context.
     */
    private void updateApplicationStatus(ApplicationStatus status) {
        int row = applicantTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an applicant first.");
            return;
        }
        // Accepting and rejecting are final enough to ask for confirmation; shortlisting stays lightweight.
        if ((status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED)
                && !UiMessage.confirm(this, "Are you sure you want to mark this application as " + status + "?", "Confirm Review Action")) {
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(row, 0));
        int jobRow = jobTable.getSelectedRow();
        String selectedJobId = jobRow >= 0 ? String.valueOf(jobTableModel.getValueAt(jobRow, 0)) : null;
        try {
            // Reviewer notes are saved with the status change so decisions have context later.
            applicationService.updateStatus(applicationId, status, reviewArea.getText().trim());
            UiMessage.info(this, "Application updated to " + status + ".");
            refreshJobs();
            if (selectedJobId != null) {
                selectJobRow(selectedJobId);
                loadApplicantsForJob(selectedJobId);
            }
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Cancels an accepted application and reopens the job slot managed by the service layer.
     */
    private void cancelAcceptedApplication() {
        int row = applicantTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an applicant first.");
            return;
        }
        if (!UiMessage.confirm(this, "Are you sure you want to cancel this accepted application and reopen the slot?", "Confirm Cancellation")) {
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(row, 0));
        int jobRow = jobTable.getSelectedRow();
        String selectedJobId = jobRow >= 0 ? String.valueOf(jobTableModel.getValueAt(jobRow, 0)) : null;
        try {
            // Cancelling acceptance is handled by the service because it may also reopen the job.
            applicationService.cancelAcceptance(applicationId, reviewArea.getText().trim());
            UiMessage.info(this, "Accepted application cancelled and job reopened.");
            refreshJobs();
            if (selectedJobId != null) {
                selectJobRow(selectedJobId);
                loadApplicantsForJob(selectedJobId);
            }
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Reloads applicants for one job, applying the current filter and sort order.
     */
    private void loadApplicantsForJob(String jobId) {
        JobPosting job = findJob(jobId).orElse(null);
        if (job == null) {
            // If the job disappeared, clear dependent panels so no old applicant details remain on screen.
            loadedApplicantJobId = null;
            applicantTableModel.setRowCount(0);
            reviewArea.setText("");
            applicantSummaryArea.setText("");
            matchInfoArea.setText("");
            updateApplicantEmptyState();
            UiMessage.error(this, "This job no longer exists. Please refresh the job list.");
            refreshJobs();
            return;
        }
        if (!canManageModule(job.getModuleCode())) {
            UiMessage.error(this, "You can only review applicants for your assigned modules.");
            return;
        }
        // From this point, filter/sort changes can reload this same job through loadedApplicantJobId.
        loadedApplicantJobId = jobId;
        applicantTableModel.setRowCount(0);
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        matchInfoArea.setText("");
        // Filtering and sorting happen before table rendering so table rows stay simple display data.
        List<ApplicationRecord> applications = applicationService.getApplicationsForJob(jobId).stream()
                .filter(this::matchesApplicantFilter)
                .sorted(this::compareApplications)
                .toList();
        for (ApplicationRecord application : applications) {
            ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
            // Deleted applicant profiles are still shown so historical application records are not hidden.
            applicantTableModel.addRow(new Object[]{
                    application.getApplicationId(),
                    applicant == null ? "[Deleted Applicant]" : valueOrDash(applicant.getName()),
                    application.getStatus(),
                    application.getMatchScore(),
                    String.join(", ", application.getMissingSkills())
            });
        }
        updateApplicantEmptyState();
    }

    /**
     * Reselects a job row after table refreshes, if the job still exists.
     */
    private void selectJobRow(String jobId) {
        for (int i = 0; i < jobTableModel.getRowCount(); i++) {
            if (jobId.equals(String.valueOf(jobTableModel.getValueAt(i, 0)))) {
                jobTable.setRowSelectionInterval(i, i);
                return;
            }
        }
        jobTable.clearSelection();
    }

    private void returnToLogin() {
        // Normal MO users return to login; admins simply close this child window in goBack().
        new LoginFrame(dataService).setVisible(true);
        dispose();
    }

    /**
     * Builds the lower review panel containing applicant details and editable reviewer notes.
     */
    private JPanel buildReviewBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setOpaque(false);

        JPanel summaryCard = UiTheme.createCard("Applicant Summary", "Quick reference for the selected applicant.");
        summaryCard.add(wrapArea(applicantSummaryArea), BorderLayout.CENTER);

        JPanel notesCard = UiTheme.createCard("Reviewer Notes", "Add notes before shortlisting, accepting, rejecting, or cancelling acceptance.");
        notesCard.add(wrapArea(reviewArea), BorderLayout.CENTER);

        panel.add(summaryCard);
        panel.add(notesCard);
        return panel;
    }

    /**
     * Formats applicant details into a readable block for the reviewer.
     */
    private String buildApplicantSummary(ApplicantProfile applicant, ApplicationRecord application, JobPosting job) {
        return "Name: " + valueOrDash(applicant.getName()) + "\n" +
                "Email: " + valueOrDash(applicant.getEmail()) + "\n" +
                "Phone: " + valueOrDash(applicant.getPhone()) + "\n" +
                "Availability: " + valueOrDash(applicant.getAvailability()) + "\n" +
                "Preferred Duties: " + valueOrDash(applicant.getPreferredDuties()) + "\n" +
                "Application Status: " + application.getStatus() + "\n" +
                "Match Score: " + application.getMatchScore() + "%\n" +
                "Missing Skills: " + valueOrDash(String.join(", ", application.getMissingSkills())) + "\n" +
                "CV Path: " + valueOrDash(applicant.getCvPath()) + "\n" +
                "For Job: " + job.getModuleCode() + " - " + job.getModuleTitle() + "\n\n" +
                "Experience Summary:\n" + valueOrDash(applicant.getExperienceSummary());
    }

    private String valueOrDash(String value) {
        // Keeps summary panels readable when optional profile fields are missing.
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * Applies shared UI styling and table renderers after Swing components are created.
     */
    private void styleComponents() {
        UiTheme.styleTextField(jobIdField);
        UiTheme.styleComboBox(moduleCodeBox);
        UiTheme.styleTextField(moduleTitleField);
        UiTheme.styleTextField(hoursField);
        UiTheme.styleTextField(requiredTaCountField);
        UiTheme.styleTextField(skillsField);
        UiTheme.styleTextField(deadlineField);
        UiTheme.styleComboBox(statusBox);
        UiTheme.styleTextArea(dutiesArea, 5);
        UiTheme.styleTextArea(reviewArea, 4);
        UiTheme.styleTextArea(applicantSummaryArea, 8);
        UiTheme.styleTextArea(matchInfoArea, 8);
        UiTheme.styleComboBox(applicantStatusFilter);
        UiTheme.styleComboBox(applicantSortBox);
        UiTheme.styleTable(jobTable);
        UiTheme.styleTable(applicantTable);
        // Custom renderers add quick visual cues without changing the underlying table values.
        jobTable.getColumnModel().getColumn(4).setCellRenderer(new DeadlineWarningRenderer());
        jobTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        applicantTable.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer());
        UiTheme.setColumnWidths(jobTable, 100, 300, 80, 110, 140, 100);
        UiTheme.setColumnWidths(applicantTable, 130, 170, 120, 90, 220);
        applicantSummaryArea.setEditable(false);
    }

    private JScrollPane wrapArea(JTextArea area) {
        // Text areas are always wrapped in themed scroll panes for consistent spacing and borders.
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
    }

    /**
     * Returns all jobs visible to this user.
     */
    private List<JobPosting> getScopedJobs() {
        return jobService.getAllJobs().stream()
                .filter(job -> canManageModule(job.getModuleCode()))
                .collect(Collectors.toList());
    }

    /**
     * Builds the header summary explaining which modules this screen can manage.
     */
    private String buildScopeSummary() {
        if (managedModuleCodes.isEmpty()) {
            return adminMode
                    ? "No module codes are available yet. Create an MO account or add a managed module first."
                    : "This MO account is not assigned to any modules yet. Ask the admin to bind modules before posting jobs.";
        }
        if (adminMode) {
            return "Admin access includes all known modules: " + String.join(", ", managedModuleCodes) + ".";
        }
        return "Publish jobs, review applicants, and manage hiring decisions for: "
                + String.join(", ", managedModuleCodes) + ".";
    }

    private void goBack() {
        if (adminMode) {
            dispose();
            return;
        }
        returnToLogin();
    }

    /**
     * Populates the module selector from the resolved permission scope.
     */
    private void populateManagedModules() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String moduleCode : managedModuleCodes) {
            model.addElement(moduleCode);
        }
        moduleCodeBox.setModel(model);
    }

    private boolean canManageModule(String moduleCode) {
        // Admin mode bypasses per-user module checks; normal users rely on their assigned module list.
        return adminMode || currentUser.managesModule(moduleCode);
    }

    /**
     * Resolves module access for the current user.
     *
     * Admins do not have a single assigned list, so their scope is collected
     * from existing MO assignments and job postings.
     */
    private List<String> resolveManagedModuleCodes() {
        if (!adminMode) {
            return currentUser.getManagedModuleCodes();
        }

        // LinkedHashSet preserves discovery order while removing duplicates from users and jobs.
        LinkedHashSet<String> moduleCodes = new LinkedHashSet<>();
        dataService.getUserRepository().findAll().stream()
                .flatMap(user -> user.getManagedModuleCodes().stream())
                .map(validationService::normalizeModuleCode)
                .filter(code -> !code.isBlank())
                .forEach(moduleCodes::add);
        jobService.getAllJobs().stream()
                .map(JobPosting::getModuleCode)
                .map(validationService::normalizeModuleCode)
                .filter(code -> !code.isBlank())
                .forEach(moduleCodes::add);
        return List.copyOf(moduleCodes);
    }

    /**
     * Keeps the form aligned with the module selected from the combo box.
     */
    private void bindFormSync() {
        moduleCodeBox.addActionListener(event -> {
            if (!syncingForm) {
                syncFormToSelectedModule();
            }
        });
    }

    /**
     * When a module is selected, load its existing job if one exists; otherwise
     * prepare the form for a new job under that module.
     */
    private void syncFormToSelectedModule() {
        String moduleCode = String.valueOf(moduleCodeBox.getSelectedItem()).trim();
        if (moduleCode.isBlank()) {
            return;
        }

        Optional<JobPosting> existingJob = getScopedJobs().stream()
                .filter(job -> moduleCode.equals(job.getModuleCode()))
                .findFirst();

        // One module maps to one active editor state here; selecting the module loads that job immediately.
        if (existingJob.isPresent()) {
            applyJobToForm(existingJob.get());
            selectJobRow(existingJob.get().getJobId());
            return;
        }

        syncingForm = true;
        // No job exists for this module yet, so keep the module and reset the rest of the form.
        jobTable.clearSelection();
        applicantTable.clearSelection();
        jobIdField.setForeground(Color.GRAY);
        jobIdField.setText(NEW_JOB_PLACEHOLDER);
        statusBox.setSelectedItem(JobStatus.OPEN);
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        matchInfoArea.setText("");
        applicantTableModel.setRowCount(0);
        loadedApplicantJobId = null;
        updateApplicantEmptyState();
        syncingForm = false;
    }

    /**
     * Copies a JobPosting into the editor and clears applicant details that
     * belonged to any previously selected job.
     */
    private void applyJobToForm(JobPosting job) {
        syncingForm = true;
        // Existing jobs show their real ID in black to distinguish them from new unsaved postings.
        jobIdField.setForeground(Color.BLACK);
        jobIdField.setText(job.getJobId());
        moduleCodeBox.setSelectedItem(job.getModuleCode());
        moduleTitleField.setText(job.getModuleTitle());
        hoursField.setText(String.valueOf(job.getHours()));
        requiredTaCountField.setText(String.valueOf(job.getRequiredTaCount()));
        skillsField.setText(String.join(", ", job.getRequiredSkills()));
        deadlineField.setText(String.valueOf(job.getApplicationDeadline()));
        statusBox.setSelectedItem(job.getStatus());
        dutiesArea.setText(job.getDuties());
        matchInfoArea.setText("");
        applicantSummaryArea.setText("");
        reviewArea.setText("");
        applicantTableModel.setRowCount(0);
        loadedApplicantJobId = null;
        updateApplicantEmptyState();
        syncingForm = false;
    }

    /**
     * Saves an open job. If the job was previously closed, the reviewer may
     * need to remove accepted TAs so at least one slot becomes available again.
     */
    private boolean handleOpeningJob(JobPosting jobPosting, JobPosting existingJob) {
        if (existingJob == null || existingJob.getStatus() != JobStatus.CLOSED) {
            jobService.saveJob(jobPosting);
            return true;
        }

        // Reopening a previously closed job may require cancelling accepted applications first.
        List<ApplicationRecord> acceptedApplications = applicationService.getApplicationsForJob(existingJob.getJobId()).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .collect(Collectors.toList());
        // Reopening only needs enough accepted TAs removed to create at least one open slot again.
        int minimumToCancel = Math.max(0, acceptedApplications.size() - jobPosting.getRequiredTaCount() + 1);

        if (minimumToCancel == 0) {
            jobService.saveJob(jobPosting);
            return true;
        }

        // A null result means the reviewer cancelled the dialog, so the save is abandoned.
        List<ApplicationRecord> selectedApplicants = promptForAcceptedTaRemoval(jobPosting, acceptedApplications, minimumToCancel);
        if (selectedApplicants == null) {
            return false;
        }

        jobService.saveJob(jobPosting);
        String reviewerNotes = reviewArea.getText().trim();
        for (ApplicationRecord application : selectedApplicants) {
            applicationService.cancelAcceptance(application.getApplicationId(), reviewerNotes);
        }
        return true;
    }

    /**
     * Saves a closed job. If there are not enough accepted applicants yet, the
     * reviewer must pick applicants to accept before the job can be closed.
     */
    private boolean handleClosingJob(JobPosting jobPosting, JobPosting existingJob) {
        if (existingJob == null) {
            UiMessage.error(this, "Save the job as OPEN first, then select TAs before closing it.");
            return false;
        }

        // Existing accepted applications count toward the required TA number before asking for more.
        List<ApplicationRecord> applications = applicationService.getApplicationsForJob(existingJob.getJobId());
        int acceptedCount = (int) applications.stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        // Closing fills only the remaining vacancies; already accepted TAs stay accepted.
        int additionalNeeded = Math.max(0, jobPosting.getRequiredTaCount() - acceptedCount);

        if (additionalNeeded > 0) {
            List<ApplicationRecord> selectedApplicants = promptForTaSelection(jobPosting, applications, additionalNeeded);
            if (selectedApplicants == null) {
                return false;
            }

            // Save as OPEN first so each acceptance can update demand/status through ApplicationService rules.
            jobPosting.setStatus(JobStatus.OPEN);
            jobService.saveJob(jobPosting);

            String reviewerNotes = reviewArea.getText().trim();
            for (ApplicationRecord application : selectedApplicants) {
                applicationService.updateStatus(application.getApplicationId(), ApplicationStatus.ACCEPTED, reviewerNotes);
            }
            return true;
        }

        jobService.saveJob(jobPosting);
        return true;
    }

    /**
     * Shows a dialog for selecting accepted applicants who should be removed
     * when reopening a fully staffed job.
     */
    private List<ApplicationRecord> promptForAcceptedTaRemoval(JobPosting jobPosting,
                                                               List<ApplicationRecord> acceptedApplications,
                                                               int minimumSelectionCount) {
        // Loop until the user either cancels or selects enough accepted TAs to free a slot.
        while (true) {
            JList<ApplicantSelectionItem> selectionList = buildApplicantSelectionList(
                    acceptedApplications.stream()
                            .map(this::toApplicantSelectionItem)
                            .collect(Collectors.toList())
            );

            JTextArea helperText = new JTextArea(
                    "Select at least " + minimumSelectionCount + " accepted TA(s) to remove before reopening "
                            + jobPosting.getModuleCode() + " - " + jobPosting.getModuleTitle() + "."
            );
            helperText.setEditable(false);
            helperText.setOpaque(false);
            helperText.setLineWrap(true);
            helperText.setWrapStyleWord(true);
            helperText.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            JScrollPane selectionScrollPane = new JScrollPane(selectionList);
            UiTheme.styleScrollPane(selectionScrollPane);

            JPanel content = new JPanel(new BorderLayout(0, 8));
            content.add(helperText, BorderLayout.NORTH);
            content.add(selectionScrollPane, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    content,
                    "Select TA to Remove",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            // The dialog allows multi-select, but this validation enforces the required minimum.
            List<ApplicantSelectionItem> selectedValues = selectionList.getSelectedValuesList();
            if (selectedValues.size() < minimumSelectionCount) {
                UiMessage.error(this, "Please select at least " + minimumSelectionCount + " accepted TA(s) to reopen this job.");
                continue;
            }

            return selectedValues.stream()
                    .map(ApplicantSelectionItem::application)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Shows a dialog for selecting the exact number of applicants needed to fill
     * the remaining TA vacancies before closing a job.
     */
    private List<ApplicationRecord> promptForTaSelection(JobPosting jobPosting,
                                                         List<ApplicationRecord> applications,
                                                         int additionalNeeded) {
        // Only active, non-accepted applicants can be chosen to fill remaining TA slots.
        List<ApplicantSelectionItem> candidates = applications.stream()
                .filter(application -> application.getStatus() != ApplicationStatus.REJECTED)
                .filter(application -> application.getStatus() != ApplicationStatus.ACCEPTED)
                .filter(application -> application.getStatus() != ApplicationStatus.WITHDRAWN)
                .map(this::toApplicantSelectionItem)
                .collect(Collectors.toList());

        if (candidates.size() < additionalNeeded) {
            UiMessage.error(this, "Not enough available applicants to close this job. Please shortlist more TAs first.");
            return null;
        }

        // Closing requires an exact number so the final accepted count matches requiredTaCount.
        while (true) {
            JList<ApplicantSelectionItem> selectionList = buildApplicantSelectionList(candidates);

            JTextArea helperText = new JTextArea(
                    "Select exactly " + additionalNeeded + " TA(s) to close "
                            + jobPosting.getModuleCode() + " - " + jobPosting.getModuleTitle() + "."
            );
            helperText.setEditable(false);
            helperText.setOpaque(false);
            helperText.setLineWrap(true);
            helperText.setWrapStyleWord(true);
            helperText.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            JScrollPane selectionScrollPane = new JScrollPane(selectionList);
            UiTheme.styleScrollPane(selectionScrollPane);

            JPanel content = new JPanel(new BorderLayout(0, 8));
            content.add(helperText, BorderLayout.NORTH);
            content.add(selectionScrollPane, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    content,
                    "Select TA for Closing Job",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            // Exact-count validation keeps accidental over-selection from accepting too many TAs.
            List<ApplicantSelectionItem> selectedValues = selectionList.getSelectedValuesList();
            if (selectedValues.size() != additionalNeeded) {
                UiMessage.error(this, "Please select exactly " + additionalNeeded + " TA(s).");
                continue;
            }

            return selectedValues.stream()
                    .map(ApplicantSelectionItem::application)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Converts an application into a compact display item for selection dialogs.
     */
    private ApplicantSelectionItem toApplicantSelectionItem(ApplicationRecord application) {
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        String applicantName = applicant == null ? "[Deleted Applicant]" : valueOrDash(applicant.getName());
        String applicantEmail = applicant == null ? "-" : valueOrDash(applicant.getEmail());
        String label = applicantName
                + " | " + applicantEmail
                + " | " + application.getStatus()
                + " | Match " + application.getMatchScore() + "%";
        return new ApplicantSelectionItem(application, label);
    }

    /**
     * Creates the reusable multi-select list used by TA selection dialogs.
     */
    private JList<ApplicantSelectionItem> buildApplicantSelectionList(List<ApplicantSelectionItem> items) {
        DefaultListModel<ApplicantSelectionItem> model = new DefaultListModel<>();
        for (ApplicantSelectionItem item : items) {
            model.addElement(item);
        }
        JList<ApplicantSelectionItem> selectionList = new JList<>(model);
        selectionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Keep the dialog usable for both small and larger candidate lists.
        selectionList.setVisibleRowCount(Math.min(8, Math.max(4, items.size())));
        selectionList.setCellRenderer(new DefaultListCellRenderer());
        return selectionList;
    }

    /**
     * Displays accepted TA count against required TA count, capped at demand.
     */
    private String buildTaDemandText(JobPosting job) {
        int acceptedCount = applicationService.getAcceptedCountForJob(job.getJobId());
        return Math.min(acceptedCount, job.getRequiredTaCount()) + "/" + job.getRequiredTaCount();
    }

    /**
     * Checks whether an application should remain visible under the selected status filter.
     */
    private boolean matchesApplicantFilter(ApplicationRecord application) {
        String selected = String.valueOf(applicantStatusFilter.getSelectedItem());
        return "All Statuses".equals(selected) || application.getStatus().name().equals(selected);
    }

    /**
     * Sorts applications according to the current reviewer-selected option.
     */
    private int compareApplications(ApplicationRecord left, ApplicationRecord right) {
        String selected = String.valueOf(applicantSortBox.getSelectedItem());
        return switch (selected) {
            case "Match % (Low to High)" -> Integer.compare(left.getMatchScore(), right.getMatchScore());
            case "Applicant Name (A-Z)" -> applicantNameFor(left).compareToIgnoreCase(applicantNameFor(right));
            case "Applicant Name (Z-A)" -> applicantNameFor(right).compareToIgnoreCase(applicantNameFor(left));
            case "Status" -> left.getStatus().name().compareTo(right.getStatus().name());
            default -> Integer.compare(right.getMatchScore(), left.getMatchScore());
        };
    }

    /**
     * Looks up the applicant name used by name-based sorting.
     */
    private String applicantNameFor(ApplicationRecord application) {
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        if (applicant == null || applicant.getName() == null) {
            return "";
        }
        return applicant.getName();
    }

    private Optional<JobPosting> findJob(String jobId) {
        // Repository lookups are wrapped here to keep callers concise.
        return dataService.getJobRepository().findById(jobId);
    }

    private Optional<ApplicantProfile> findApplicant(String applicantId) {
        // Applicant profiles are separate from application records, so missing profiles are possible.
        return dataService.getProfileRepository().findByApplicantId(applicantId);
    }

    /**
     * Updates the applicant table's empty message based on whether a job is loaded
     * and whether a status filter is active.
     */
    private void updateApplicantEmptyState() {
        if (loadedApplicantJobId == null) {
            applicantTable.setEmptyMessage("Select a job and click Load Applicants to review submissions.");
            return;
        }

        String selected = String.valueOf(applicantStatusFilter.getSelectedItem());
        if ("All Statuses".equals(selected)) {
            applicantTable.setEmptyMessage("No applicants have applied to this job yet.");
            return;
        }

        applicantTable.setEmptyMessage("No applicants match the current filter.");
    }

    /**
     * Finds a job's row in the current table model.
     */
    private int findJobRow(String jobId) {
        for (int i = 0; i < jobTableModel.getRowCount(); i++) {
            if (jobId.equals(String.valueOf(jobTableModel.getValueAt(i, 0)))) {
                return i;
            }
        }
        return -1;
    }

    private record ApplicantSelectionItem(ApplicationRecord application, String label) {
        @Override
        public String toString() {
            // JList uses toString() for the visible label when no custom text extractor is provided.
            return label;
        }
    }

    /**
     * Highlights deadlines in the job table so urgent or overdue postings stand out.
     */
    private static class DeadlineWarningRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value instanceof LocalDate deadline) {
                // The warning colors are intentionally simple: red for overdue, yellow for the last 3 days.
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
     * Renders application and job statuses with background colors for quick scanning.
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
                case "WITHDRAWN", "CLOSED" -> label.setBackground(new Color(234, 234, 234));
                case "OPEN" -> label.setBackground(new Color(232, 240, 255));
                default -> label.setBackground(Color.WHITE);
            }
            return label;
        }
    }
}
