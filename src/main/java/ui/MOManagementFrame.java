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
import java.time.format.DateTimeParseException;
import java.time.LocalDate;

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

    private final JTextField jobIdField = new JTextField();
    private final JTextField moduleCodeField = new JTextField();
    private final JTextField moduleTitleField = new JTextField();
    private final JTextField hoursField = new JTextField();
    private final JTextField skillsField = new JTextField();
    private final JTextField deadlineField = new JTextField();
    private final JComboBox<JobStatus> statusBox = new JComboBox<>(JobStatus.values());
    private final JTextArea dutiesArea = new JTextArea(3, 20);
    private final JTextArea reviewArea = new JTextArea(4, 20);
    private final JTextArea matchInfoArea = new JTextArea(6, 20);
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Hours", "Deadline", "Status"}, 0);
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
        root.add(UiTheme.createHeader("Module Organiser Console", "Publish jobs, review applicants, and manage hiring decisions from one workspace."), BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        add(UiTheme.wrapPage(root));

        refreshJobs();
        clearForm();
    }

    private JPanel buildFormPanel() {
        JPanel panel = UiTheme.createCard("Job Posting Editor", "Select an existing job to revise it, or start a new vacancy from a clean form.");

        JPanel form = UiTheme.createFormGrid();
        jobIdField.setEditable(false);
        jobIdField.setForeground(Color.GRAY);
        matchInfoArea.setEditable(false);
        UiTheme.addFormRow(form, 0, "Job ID", jobIdField);
        UiTheme.addFormRow(form, 2, "Module Code", moduleCodeField);
        UiTheme.addFormRow(form, 4, "Module Title", moduleTitleField);
        UiTheme.addFormRow(form, 6, "Hours", hoursField);
        UiTheme.addFormRow(form, 8, "Required Skills", skillsField);
        UiTheme.addFormRow(form, 10, "Deadline (YYYY-MM-DD)", deadlineField);
        UiTheme.addFormRow(form, 12, "Status", statusBox);
        UiTheme.addFormRow(form, 14, "Duties", wrapArea(dutiesArea));

        JPanel lower = UiTheme.createCard("Applicant Match Details", "Review fit, missing skills, and applicant notes for the selected submission.");
        lower.add(wrapArea(matchInfoArea), BorderLayout.CENTER);

        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton newButton = UiTheme.createSecondaryButton("New Job");
        JButton saveButton = UiTheme.createPrimaryButton("Save Job");

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
        body.add(wrapArea(reviewArea), BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private void refreshJobs() {
        jobTableModel.setRowCount(0);
        for (JobPosting job : jobService.getAllJobs()) {
            jobTableModel.addRow(new Object[]{
                    job.getJobId(),
                    job.getModuleCode() + " - " + job.getModuleTitle(),
                    job.getHours(),
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
        JobPosting job = jobService.getJobById(String.valueOf(jobTableModel.getValueAt(row, 0)));
        jobIdField.setForeground(Color.BLACK);
        jobIdField.setText(job.getJobId());
        moduleCodeField.setText(job.getModuleCode());
        moduleTitleField.setText(job.getModuleTitle());
        hoursField.setText(String.valueOf(job.getHours()));
        skillsField.setText(String.join(", ", job.getRequiredSkills()));
        deadlineField.setText(String.valueOf(job.getApplicationDeadline()));
        statusBox.setSelectedItem(job.getStatus());
        dutiesArea.setText(job.getDuties());
        matchInfoArea.setText("");
    }

    private void clearForm() {
        jobTable.clearSelection();
        applicantTable.clearSelection();
        jobIdField.setForeground(Color.GRAY);
        jobIdField.setText(NEW_JOB_PLACEHOLDER);
        moduleCodeField.setText("");
        moduleTitleField.setText("");
        hoursField.setText("");
        skillsField.setText("");
        deadlineField.setText("");
        dutiesArea.setText("");
        statusBox.setSelectedItem(JobStatus.OPEN);
        matchInfoArea.setText("");
        reviewArea.setText("");
        applicantTableModel.setRowCount(0);
        moduleCodeField.requestFocusInWindow();
    }

    private void saveJob() {
        try {
            String jobId = jobIdField.getText().trim();
            JobPosting existingJob = NEW_JOB_PLACEHOLDER.equals(jobId) || jobId.isBlank()
                    ? null
                    : jobService.getJobById(jobId);

            JobPosting jobPosting = new JobPosting();
            jobPosting.setJobId(NEW_JOB_PLACEHOLDER.equals(jobId) ? "" : jobId);
            jobPosting.setModuleCode(moduleCodeField.getText().trim());
            jobPosting.setModuleTitle(moduleTitleField.getText().trim());
            jobPosting.setHours(Integer.parseInt(hoursField.getText().trim()));
            jobPosting.setRequiredSkills(validationService.parseSkills(skillsField.getText()));
            jobPosting.setApplicationDeadline(LocalDate.parse(deadlineField.getText().trim()));
            jobPosting.setStatus((JobStatus) statusBox.getSelectedItem());
            jobPosting.setDuties(dutiesArea.getText().trim());
            jobPosting.setPostedBy(currentUser.getUserId());
            jobService.saveJob(jobPosting);
            if (existingJob != null
                    && existingJob.getStatus() == JobStatus.CLOSED
                    && jobPosting.getStatus() == JobStatus.OPEN) {
                applicationService.reopenJob(jobPosting.getJobId());
            }
            UiMessage.info(this, "Job saved successfully.");
            refreshJobs();
            clearForm();
        } catch (NumberFormatException ex) {
            UiMessage.error(this, "Hours must be a number.");
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
        applicantTableModel.setRowCount(0);
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

    private void styleComponents() {
        UiTheme.styleTextField(jobIdField);
        UiTheme.styleTextField(moduleCodeField);
        UiTheme.styleTextField(moduleTitleField);
        UiTheme.styleTextField(hoursField);
        UiTheme.styleTextField(skillsField);
        UiTheme.styleTextField(deadlineField);
        UiTheme.styleComboBox(statusBox);
        UiTheme.styleTextArea(dutiesArea, 5);
        UiTheme.styleTextArea(reviewArea, 4);
        UiTheme.styleTextArea(matchInfoArea, 8);
        UiTheme.styleTable(jobTable);
        UiTheme.styleTable(applicantTable);
        UiTheme.setColumnWidths(jobTable, 100, 300, 80, 140, 100);
        UiTheme.setColumnWidths(applicantTable, 130, 170, 120, 90, 220);
    }

    private JScrollPane wrapArea(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
    }
}
