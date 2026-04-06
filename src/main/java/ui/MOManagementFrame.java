package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.JobPosting;
import model.JobStatus;
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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MO dashboard for posting jobs and reviewing applicants.
 */
public class MOManagementFrame extends JFrame {
    private static final String NEW_JOB_PLACEHOLDER = "AUTO-GENERATED ON SAVE";

    private final DataService dataService;
    private final JobService jobService;
    private final ApplicantService applicantService;
    private final ApplicationService applicationService;
    private final MatchingService matchingService;
    private final ValidationService validationService;
    private final User currentUser;
    private final List<String> managedModuleCodes;
    private boolean syncingForm;

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
    private final JTextArea applicantSummaryArea = new JTextArea(4, 20);
    private final JTextArea matchInfoArea = new JTextArea(6, 20);
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Hours", "TA Demand", "Deadline", "Status"}, 0);
    private final JTable jobTable = new JTable(jobTableModel);
    private final DefaultTableModel applicantTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Applicant", "Status", "Match %", "Missing"}, 0);
    private final JTable applicantTable = new JTable(applicantTableModel);

    public MOManagementFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;
        this.validationService = new ValidationService();
        this.jobService = new JobService(dataService.getJobRepository(), validationService);
        this.applicantService = new ApplicantService(dataService.getProfileRepository(), validationService);
        this.matchingService = new MatchingService();
        this.applicationService = new ApplicationService(
                dataService.getApplicationRepository(),
                dataService.getJobRepository(),
                matchingService
        );
        this.managedModuleCodes = currentUser.getManagedModuleCodes();

        setTitle("MO Management - " + Constants.APP_TITLE);
        setSize(1420, 880);
        setMinimumSize(new Dimension(1020, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);
        styleComponents();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildFormPanel(), buildTablesPanel());
        splitPane.setResizeWeight(0.4);
        UiTheme.styleSplitPane(splitPane);

        JPanel root = UiTheme.createPagePanel();
        root.add(UiTheme.createHeader(
                "Module Organiser Console",
                buildScopeSummary()
        ), BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        add(UiTheme.wrapPage(root));

        refreshJobs();
        clearForm();
        bindFormSync();
    }

    private JPanel buildFormPanel() {
        JPanel panel = UiTheme.createCard("Job Posting Editor", "Select an existing job to revise it, or start a new vacancy from a clean form.");

        JPanel form = UiTheme.createFormGrid();
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

        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton newButton = UiTheme.createSecondaryButton("New Job");
        JButton saveButton = UiTheme.createPrimaryButton("Save Job");

        boolean hasManagedModules = !managedModuleCodes.isEmpty();
        newButton.setEnabled(hasManagedModules);
        saveButton.setEnabled(hasManagedModules);
        moduleCodeBox.setEnabled(hasManagedModules);

        backButton.addActionListener(event -> returnToLogin());
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

    private JPanel buildTablesPanel() {
        JPanel panel = UiTheme.createCard("Review Queue", "Inspect job postings on the top table and applicant submissions below.");

        JButton loadApplicantsButton = UiTheme.createSecondaryButton("Load Applicants");
        JButton shortlistButton = UiTheme.createSecondaryButton("Shortlist");
        JButton acceptButton = UiTheme.createPrimaryButton("Accept");
        JButton cancelAcceptanceButton = UiTheme.createSecondaryButton("Cancel Acceptance");
        JButton rejectButton = UiTheme.createDangerButton("Reject");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");

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
        });

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

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UiTheme.createButtonRow(FlowLayout.LEFT, loadApplicantsButton, shortlistButton, acceptButton, cancelAcceptanceButton, rejectButton, refreshButton), BorderLayout.NORTH);
        body.add(splitPane, BorderLayout.CENTER);
        body.add(buildReviewBottomPanel(), BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private void refreshJobs() {
        jobTableModel.setRowCount(0);
        for (JobPosting job : getScopedJobs()) {
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

    private void loadSelectedJobToForm() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        applyJobToForm(jobService.getJobById(String.valueOf(jobTableModel.getValueAt(row, 0))));
    }

    private void clearForm() {
        syncingForm = true;
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
        moduleCodeBox.requestFocusInWindow();
        syncingForm = false;
    }

    private void saveJob() {
        try {
            if (managedModuleCodes.isEmpty()) {
                UiMessage.error(this, "This MO is not assigned to any modules yet.");
                return;
            }
            String jobId = jobIdField.getText().trim();
            JobPosting existingJob = NEW_JOB_PLACEHOLDER.equals(jobId) || jobId.isBlank()
                    ? null
                    : jobService.getJobById(jobId);
            String moduleCode = String.valueOf(moduleCodeBox.getSelectedItem()).trim();
            if (!currentUser.managesModule(moduleCode)) {
                throw new IllegalArgumentException("You can only manage TA hiring for your assigned modules.");
            }
            if (existingJob != null && !currentUser.managesModule(existingJob.getModuleCode())) {
                throw new IllegalArgumentException("You can only edit jobs for your assigned modules.");
            }

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
            refreshJobs();
            clearForm();
        } catch (NumberFormatException ex) {
            UiMessage.error(this, "Hours and required TA count must be numbers.");
        } catch (DateTimeParseException ex) {
            UiMessage.error(this, "Deadline must use the format YYYY-MM-DD.");
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    private void loadApplicantsForSelectedJob() {
        int row = jobTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select a job first.");
            return;
        }
        String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
        loadApplicantsForJob(jobId);
    }

    private void showSelectedApplicantMatch() {
        int applicationRow = applicantTable.getSelectedRow();
        int jobRow = jobTable.getSelectedRow();
        if (applicationRow < 0 || jobRow < 0) {
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(applicationRow, 0));
        String jobId = String.valueOf(jobTableModel.getValueAt(jobRow, 0));
        ApplicationRecord application = applicationService.getApplicationsForJob(jobId).stream()
                .filter(item -> item.getApplicationId().equals(applicationId))
                .findFirst()
                .orElse(null);
        if (application == null) {
            return;
        }
        ApplicantProfile applicant = applicantService.getProfileByApplicantId(application.getApplicantId());
        JobPosting job = jobService.getJobById(jobId);
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

    private void updateApplicationStatus(ApplicationStatus status) {
        int row = applicantTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an applicant first.");
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(row, 0));
        int jobRow = jobTable.getSelectedRow();
        String selectedJobId = jobRow >= 0 ? String.valueOf(jobTableModel.getValueAt(jobRow, 0)) : null;
        try {
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

    private void cancelAcceptedApplication() {
        int row = applicantTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an applicant first.");
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(row, 0));
        int jobRow = jobTable.getSelectedRow();
        String selectedJobId = jobRow >= 0 ? String.valueOf(jobTableModel.getValueAt(jobRow, 0)) : null;
        try {
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

    private void loadApplicantsForJob(String jobId) {
        JobPosting job = jobService.getJobById(jobId);
        if (!currentUser.managesModule(job.getModuleCode())) {
            UiMessage.error(this, "You can only review applicants for your assigned modules.");
            return;
        }
        applicantTableModel.setRowCount(0);
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        matchInfoArea.setText("");
        for (ApplicationRecord application : applicationService.getApplicationsForJob(jobId)) {
            ApplicantProfile applicant = applicantService.getProfileByApplicantId(application.getApplicantId());
            applicantTableModel.addRow(new Object[]{
                    application.getApplicationId(),
                    applicant.getName(),
                    application.getStatus(),
                    application.getMatchScore(),
                    String.join(", ", application.getMissingSkills())
            });
        }
    }

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
        new LoginFrame(dataService).setVisible(true);
        dispose();
    }

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
        return value == null || value.isBlank() ? "-" : value;
    }

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
        UiTheme.styleTable(jobTable);
        UiTheme.styleTable(applicantTable);
        UiTheme.setColumnWidths(jobTable, 100, 300, 80, 110, 140, 100);
        UiTheme.setColumnWidths(applicantTable, 130, 170, 120, 90, 220);
        applicantSummaryArea.setEditable(false);
    }

    private JScrollPane wrapArea(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
    }

    private List<JobPosting> getScopedJobs() {
        return jobService.getAllJobs().stream()
                .filter(job -> currentUser.managesModule(job.getModuleCode()))
                .collect(Collectors.toList());
    }

    private String buildScopeSummary() {
        if (managedModuleCodes.isEmpty()) {
            return "This MO account is not assigned to any modules yet. Ask the admin to bind modules before posting jobs.";
        }
        return "Publish jobs, review applicants, and manage hiring decisions for: "
                + String.join(", ", managedModuleCodes) + ".";
    }

    private void populateManagedModules() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String moduleCode : managedModuleCodes) {
            model.addElement(moduleCode);
        }
        moduleCodeBox.setModel(model);
    }

    private void bindFormSync() {
        moduleCodeBox.addActionListener(event -> {
            if (!syncingForm) {
                syncFormToSelectedModule();
            }
        });
    }

    private void syncFormToSelectedModule() {
        String moduleCode = String.valueOf(moduleCodeBox.getSelectedItem()).trim();
        if (moduleCode.isBlank()) {
            return;
        }

        Optional<JobPosting> existingJob = getScopedJobs().stream()
                .filter(job -> moduleCode.equals(job.getModuleCode()))
                .findFirst();

        if (existingJob.isPresent()) {
            applyJobToForm(existingJob.get());
            selectJobRow(existingJob.get().getJobId());
            return;
        }

        syncingForm = true;
        jobTable.clearSelection();
        applicantTable.clearSelection();
        jobIdField.setForeground(Color.GRAY);
        jobIdField.setText(NEW_JOB_PLACEHOLDER);
        statusBox.setSelectedItem(JobStatus.OPEN);
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        matchInfoArea.setText("");
        applicantTableModel.setRowCount(0);
        syncingForm = false;
    }

    private void applyJobToForm(JobPosting job) {
        syncingForm = true;
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
        syncingForm = false;
    }

    private boolean handleOpeningJob(JobPosting jobPosting, JobPosting existingJob) {
        if (existingJob == null || existingJob.getStatus() != JobStatus.CLOSED) {
            jobService.saveJob(jobPosting);
            return true;
        }

        List<ApplicationRecord> acceptedApplications = applicationService.getApplicationsForJob(existingJob.getJobId()).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .collect(Collectors.toList());
        int minimumToCancel = Math.max(0, acceptedApplications.size() - jobPosting.getRequiredTaCount() + 1);

        if (minimumToCancel == 0) {
            jobService.saveJob(jobPosting);
            return true;
        }

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

    private boolean handleClosingJob(JobPosting jobPosting, JobPosting existingJob) {
        if (existingJob == null) {
            UiMessage.error(this, "Save the job as OPEN first, then select TAs before closing it.");
            return false;
        }

        List<ApplicationRecord> applications = applicationService.getApplicationsForJob(existingJob.getJobId());
        int acceptedCount = (int) applications.stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        int additionalNeeded = Math.max(0, jobPosting.getRequiredTaCount() - acceptedCount);

        if (additionalNeeded > 0) {
            List<ApplicationRecord> selectedApplicants = promptForTaSelection(jobPosting, applications, additionalNeeded);
            if (selectedApplicants == null) {
                return false;
            }

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

    private List<ApplicationRecord> promptForAcceptedTaRemoval(JobPosting jobPosting,
                                                               List<ApplicationRecord> acceptedApplications,
                                                               int minimumSelectionCount) {
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

    private List<ApplicationRecord> promptForTaSelection(JobPosting jobPosting,
                                                         List<ApplicationRecord> applications,
                                                         int additionalNeeded) {
        List<ApplicantSelectionItem> candidates = applications.stream()
                .filter(application -> application.getStatus() != ApplicationStatus.REJECTED)
                .filter(application -> application.getStatus() != ApplicationStatus.ACCEPTED)
                .map(this::toApplicantSelectionItem)
                .collect(Collectors.toList());

        if (candidates.size() < additionalNeeded) {
            UiMessage.error(this, "Not enough available applicants to close this job. Please shortlist more TAs first.");
            return null;
        }

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

    private ApplicantSelectionItem toApplicantSelectionItem(ApplicationRecord application) {
        ApplicantProfile applicant = applicantService.getProfileByApplicantId(application.getApplicantId());
        String label = applicant.getName()
                + " | " + applicant.getEmail()
                + " | " + application.getStatus()
                + " | Match " + application.getMatchScore() + "%";
        return new ApplicantSelectionItem(application, label);
    }

    private JList<ApplicantSelectionItem> buildApplicantSelectionList(List<ApplicantSelectionItem> items) {
        DefaultListModel<ApplicantSelectionItem> model = new DefaultListModel<>();
        for (ApplicantSelectionItem item : items) {
            model.addElement(item);
        }
        JList<ApplicantSelectionItem> selectionList = new JList<>(model);
        selectionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionList.setVisibleRowCount(Math.min(8, Math.max(4, items.size())));
        selectionList.setCellRenderer(new DefaultListCellRenderer());
        return selectionList;
    }

    private String buildTaDemandText(JobPosting job) {
        int acceptedCount = applicationService.getAcceptedCountForJob(job.getJobId());
        return Math.min(acceptedCount, job.getRequiredTaCount()) + "/" + job.getRequiredTaCount();
    }

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
            return label;
        }
    }
}
