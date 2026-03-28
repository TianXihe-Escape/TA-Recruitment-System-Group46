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
        setSize(1200, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        workloadTable.setDefaultRenderer(Object.class, new WorkloadRenderer());
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildTopPanel(), buildBottomPanel());
        splitPane.setResizeWeight(0.55);
        add(splitPane);

        refreshData();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        JButton loadSampleButton = new JButton("Load Sample Data");
        JButton resetButton = new JButton("Reset Demo Data");
        JButton suggestButton = new JButton("Rebalance Suggestion");
        buttonPanel.add(refreshButton);
        buttonPanel.add(loadSampleButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(suggestButton);

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

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(workloadTable), BorderLayout.CENTER);
        return panel;
    }

    private JSplitPane buildBottomPanel() {
        jobSummaryArea.setEditable(false);
        suggestionArea.setEditable(false);
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(jobSummaryArea),
                new JScrollPane(suggestionArea)
        );
        splitPane.setResizeWeight(0.5);
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
            builder.append(job.getJobId()).append(" | ")
                    .append(job.getModuleCode()).append(" ").append(job.getModuleTitle())
                    .append(" | ").append(job.getStatus())
                    .append(" | ").append(job.getHours()).append("h\n");
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
