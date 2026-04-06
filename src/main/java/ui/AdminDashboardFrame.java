package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.JobPosting;
import model.JobStatus;
import model.User;
import model.WorkloadRecord;
import service.ApplicantService;
import service.DataService;
import service.JobService;
import service.MatchingService;
import service.ValidationService;
import service.WorkloadService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin view for workload monitoring and demo data management.
 */
public class AdminDashboardFrame extends JFrame {
    private final DataService dataService;
    private final WorkloadService workloadService;
    private final MatchingService matchingService;
    private final DefaultTableModel workloadTableModel = new DefaultTableModel(
            new Object[]{"TA", "Modules", "Total Hours", "Overload"}, 0);
    private final JTable workloadTable = new JTable(workloadTableModel);
    private final JTextArea jobSummaryArea = new JTextArea();
    private final JTextArea suggestionArea = new JTextArea();

    public AdminDashboardFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        ValidationService validationService = new ValidationService();
        new ApplicantService(dataService.getProfileRepository(), validationService);
        new JobService(dataService.getJobRepository(), validationService);
        this.workloadService = new WorkloadService();
        this.matchingService = new MatchingService();

        setTitle("Admin Dashboard - " + Constants.APP_TITLE);
        setSize(1380, 860);
        setMinimumSize(new Dimension(980, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);
        styleComponents();

        workloadTable.setDefaultRenderer(Object.class, new WorkloadRenderer());
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildTopPanel(), buildBottomPanel());
        splitPane.setResizeWeight(0.55);
        UiTheme.styleSplitPane(splitPane);

        JPanel root = UiTheme.createPagePanel();
        root.add(UiTheme.createHeader("Admin Control Center", "Audit workloads, repopulate demo data, and rebalance staffing decisions."), BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        add(UiTheme.wrapPage(root));

        refreshData();
    }

    private JPanel buildTopPanel() {
        JPanel panel = UiTheme.createCard("Workload Monitor", "Track assigned hours across TAs and quickly reset or repopulate the demo environment.");

        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");
        JButton loadSampleButton = UiTheme.createSecondaryButton("Load Demo Data");
        JButton resetButton = UiTheme.createDangerButton("Reset Demo Data");
        JButton suggestButton = UiTheme.createPrimaryButton("Rebalance Suggestion");

        backButton.addActionListener(event -> returnToLogin());
        refreshButton.addActionListener(event -> refreshData());
        loadSampleButton.addActionListener(event -> {
            dataService.loadSampleData();
            refreshData();
            UiMessage.info(this, "Sample data loaded.");
        });
        resetButton.addActionListener(event -> {
            dataService.resetData();
            refreshData();
            UiMessage.info(this, "Demo data reset.");
        });
        suggestButton.addActionListener(event -> generateSuggestions());

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UiTheme.createButtonRow(FlowLayout.LEFT, backButton, refreshButton, loadSampleButton, resetButton, suggestButton), BorderLayout.NORTH);
        body.add(UiTheme.wrapTable(workloadTable), BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JSplitPane buildBottomPanel() {
        JPanel jobsCard = UiTheme.createCard("Jobs and Assignments", "High-level list of current postings, hours, and statuses.");
        jobsCard.add(wrapArea(jobSummaryArea), BorderLayout.CENTER);

        JPanel suggestionsCard = UiTheme.createCard("Rebalance Suggestions", "Use recommendations to route open jobs to lower-load applicants.");
        suggestionsCard.add(wrapArea(suggestionArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                jobsCard,
                suggestionsCard
        );
        splitPane.setResizeWeight(0.5);
        UiTheme.styleSplitPane(splitPane);
        return splitPane;
    }

    private void refreshData() {
        workloadTableModel.setRowCount(0);
        List<ApplicantProfile> profiles = dataService.getProfileRepository().findAll();
        List<JobPosting> jobs = dataService.getJobRepository().findAll();
        List<ApplicationRecord> applications = dataService.getApplicationRepository().findAll();
        int threshold = dataService.getConfig().getWorkloadThreshold();

        List<WorkloadRecord> workloads = workloadService.buildWorkloadRecords(profiles, jobs, applications, threshold);
        for (WorkloadRecord record : workloads) {
            workloadTableModel.addRow(new Object[]{
                    record.getApplicantName(),
                    String.join("; ", record.getAssignedModules()),
                    record.getTotalHours(),
                    record.isOverload() ? "YES" : "NO"
            });
        }

        StringBuilder builder = new StringBuilder("Jobs and Assignments\n\n");
        for (JobPosting job : jobs) {
            long applicationCount = applications.stream()
                    .filter(application -> job.getJobId().equals(application.getJobId()))
                    .count();
            long acceptedCount = applications.stream()
                    .filter(application -> job.getJobId().equals(application.getJobId()))
                    .filter(application -> application.getStatus() == model.ApplicationStatus.ACCEPTED)
                    .count();
            builder.append(job.getJobId()).append(" | ")
                    .append(job.getModuleCode()).append(" ").append(job.getModuleTitle())
                    .append(" | ").append(job.getStatus())
                    .append(" | ").append(job.getHours()).append("h")
                    .append(" | applicants ").append(applicationCount).append("/").append(job.getRequiredTaCount())
                    .append(" | accepted ").append(acceptedCount).append("/").append(job.getRequiredTaCount())
                    .append("\n");
        }
        jobSummaryArea.setText(builder.toString());
        suggestionArea.setText("Click 'Rebalance Suggestion' to recommend lower-load TAs for open jobs.");
    }

    private void generateSuggestions() {
        List<ApplicantProfile> profiles = dataService.getProfileRepository().findAll();
        List<JobPosting> jobs = dataService.getJobRepository().findAll();
        List<ApplicationRecord> applications = dataService.getApplicationRepository().findAll();
        int threshold = dataService.getConfig().getWorkloadThreshold();
        List<WorkloadRecord> workloads = workloadService.buildWorkloadRecords(profiles, jobs, applications, threshold);

        StringBuilder builder = new StringBuilder("Rebalance Suggestions\n\n");
        for (JobPosting job : jobs) {
            if (job.getStatus() == JobStatus.OPEN) {
                builder.append(job.getModuleCode()).append(" ").append(job.getModuleTitle()).append("\n");
                for (String suggestion : workloadService.suggestApplicantsForJob(job, profiles, workloads, matchingService)) {
                    builder.append("- ").append(suggestion).append("\n");
                }
                builder.append("\n");
            }
        }
        suggestionArea.setText(builder.toString());
    }

    private void returnToLogin() {
        new LoginFrame(dataService).setVisible(true);
        dispose();
    }

    private void styleComponents() {
        UiTheme.styleTable(workloadTable);
        UiTheme.styleTextArea(jobSummaryArea, 14);
        UiTheme.styleTextArea(suggestionArea, 14);
        UiTheme.setColumnWidths(workloadTable, 180, 420, 120, 100);
        jobSummaryArea.setEditable(false);
        suggestionArea.setEditable(false);
    }

    private JScrollPane wrapArea(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
    }

    private static class WorkloadRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Object overloadFlag = table.getValueAt(row, 3);
            if (!isSelected && "YES".equals(overloadFlag)) {
                component.setBackground(new Color(255, 224, 224));
            } else if (!isSelected) {
                component.setBackground(Color.WHITE);
            }
            return component;
        }
    }
}
