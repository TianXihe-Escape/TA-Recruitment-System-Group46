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
        setSize(1250, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildFormPanel(), buildTablesPanel());
        splitPane.setResizeWeight(0.38);
        add(splitPane);

        refreshJobs();
        clearForm();
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));
        jobIdField.setEditable(false);
        jobIdField.setForeground(Color.GRAY);
        matchInfoArea.setEditable(false);
        form.add(new JLabel("Job ID"));
        form.add(jobIdField);
        form.add(new JLabel("Module Code"));
        form.add(moduleCodeField);
        form.add(new JLabel("Module Title"));
        form.add(moduleTitleField);
        form.add(new JLabel("Hours"));
        form.add(hoursField);
        form.add(new JLabel("Required Skills"));
        form.add(skillsField);
        form.add(new JLabel("Deadline (YYYY-MM-DD)"));
        form.add(deadlineField);
        form.add(new JLabel("Status"));
        form.add(statusBox);
        form.add(new JLabel("Duties"));
        form.add(new JScrollPane(dutiesArea));

        JPanel lower = new JPanel(new BorderLayout(8, 8));
        lower.add(form, BorderLayout.NORTH);
        lower.add(new JLabel("Applicant Match Details"), BorderLayout.CENTER);
        lower.add(new JScrollPane(matchInfoArea), BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backButton = new JButton("Back to Login");
        JButton newButton = new JButton("New Job");
        JButton saveButton = new JButton("Save Job");
        buttons.add(backButton);
        buttons.add(newButton);
        buttons.add(saveButton);

        backButton.addActionListener(event -> returnToLogin());
        newButton.addActionListener(event -> clearForm());
        saveButton.addActionListener(event -> saveJob());

        panel.add(lower, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTablesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadApplicantsButton = new JButton("Load Applicants");
        JButton shortlistButton = new JButton("Shortlist");
        JButton acceptButton = new JButton("Accept");
        JButton rejectButton = new JButton("Reject");
        JButton refreshButton = new JButton("Refresh");
        topButtons.add(loadApplicantsButton);
        topButtons.add(shortlistButton);
        topButtons.add(acceptButton);
        topButtons.add(rejectButton);
        topButtons.add(refreshButton);

        loadApplicantsButton.addActionListener(event -> loadApplicantsForSelectedJob());
        shortlistButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.SHORTLISTED));
        acceptButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.ACCEPTED));
        rejectButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.REJECTED));
        refreshButton.addActionListener(event -> {
            refreshJobs();
            applicantTableModel.setRowCount(0);
            reviewArea.setText("");
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(jobTable), new JScrollPane(applicantTable));
        splitPane.setResizeWeight(0.5);
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

        panel.add(topButtons, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(new JScrollPane(reviewArea), BorderLayout.SOUTH);
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
            JobPosting jobPosting = new JobPosting();
            String jobId = jobIdField.getText().trim();
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
            UiMessage.info(this, "Job saved successfully.");
            refreshJobs();
            clearForm();
        } catch (NumberFormatException ex) {
            UiMessage.error(this, "Hours must be a number.");
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
        applicantTableModel.setRowCount(0);
        String jobId = String.valueOf(jobTableModel.getValueAt(row, 0));
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
        try {
            applicationService.updateStatus(applicationId, status, reviewArea.getText().trim());
            UiMessage.info(this, "Application updated to " + status + ".");
            refreshJobs();
            loadApplicantsForSelectedJob();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    private void returnToLogin() {
        new LoginFrame(dataService).setVisible(true);
        dispose();
    }
}
