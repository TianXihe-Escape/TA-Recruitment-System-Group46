package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.JobPosting;
import model.JobStatus;
import model.User;
import model.WorkloadRecord;
import service.ApplicantService;
import service.AuthService;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administrator dashboard for workload monitoring, demo-data maintenance,
 * and system-level account management.
 */
public class AdminDashboardFrame extends JFrame {
    /**
     * Preferred height for directory scroll areas.
     */
    private static final int DIRECTORY_PANEL_HEIGHT = 220;
    /**
     * Preferred height for the workload table card.
     */
    private static final int WORKLOAD_PANEL_HEIGHT = 120;

    /**
     * Shared data facade for repository access.
     */
    private final DataService dataService;
    /**
     * Logged-in administrator.
     */
    private final User currentUser;
    /**
     * Service that aggregates accepted assignments into workload records.
     */
    private final WorkloadService workloadService;
    /**
     * Matching helper used for rebalance suggestions.
     */
    private final MatchingService matchingService;
    /**
     * Validation helper for MO account creation.
     */
    private final ValidationService validationService;
    /**
     * Auth service used by admins to create MO accounts.
     */
    private final AuthService authService;
    private final JLabel openJobsValue = createMetricValueLabel();
    private final JLabel closedJobsValue = createMetricValueLabel();
    private final JLabel applicationCountValue = createMetricValueLabel();
    private final JLabel acceptedCountValue = createMetricValueLabel();
    private final DefaultTableModel workloadTableModel = new DefaultTableModel(
            new Object[]{"TA", "Modules", "Total Hours", "Overload"}, 0);
    private final JTable workloadTable = new PlaceholderTable(workloadTableModel, "No workload records are available yet.");
    private final JPanel taDirectoryPanel = createDirectoryListPanel();
    private final JPanel moDirectoryPanel = createDirectoryListPanel();
    private final JTextArea jobSummaryArea = new JTextArea();
    private final JTextArea suggestionArea = new JTextArea();
    private final JTextField moNameField = new JTextField();
    private final JTextField moEmailField = new JTextField();
    private final JPasswordField moPasswordField = new JPasswordField();
    private final JPasswordField moConfirmField = new JPasswordField();
    private final JTextField moModulesField = new JTextField();

    /**
     * Constructs the admin dashboard and immediately refreshes all summary data.
     */
    public AdminDashboardFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;
        this.validationService = new ValidationService();
        this.authService = new AuthService(
                dataService.getUserRepository(),
                dataService.getProfileRepository(),
                this.validationService
        );
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

    /**
     * Builds the upper half of the dashboard with metrics and control buttons.
     */
    private JPanel buildTopPanel() {
        JPanel panel = UiTheme.createCard("Workload Monitor", "Track assigned hours across TAs and quickly reset or repopulate the demo environment.");

        JButton backButton = UiTheme.createSecondaryButton("Back to Login");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");
        JButton hiringButton = UiTheme.createSecondaryButton("Open Hiring Management");
        JButton loadSampleButton = UiTheme.createSecondaryButton("Load Demo Data");
        JButton resetButton = UiTheme.createDangerButton("Reset Demo Data");
        JButton suggestButton = UiTheme.createPrimaryButton("Rebalance Suggestion");

        backButton.addActionListener(event -> returnToLogin());
        refreshButton.addActionListener(event -> refreshData());
        hiringButton.addActionListener(event -> openHiringManagement());
        loadSampleButton.addActionListener(event -> {
            dataService.loadSampleData();
            refreshData();
            UiMessage.info(this, "Sample data loaded.");
        });
        resetButton.addActionListener(event -> {
            if (!UiMessage.confirm(this, "Resetting demo data will overwrite the current JSON files. Continue?", "Confirm Reset")) {
                return;
            }
            dataService.resetData();
            refreshData();
            UiMessage.info(this, "Demo data reset.");
        });
        suggestButton.addActionListener(event -> generateSuggestions());

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(buildMetricsPanel(), BorderLayout.NORTH);
        centerPanel.add(buildOverviewPanel(), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UiTheme.createButtonRow(FlowLayout.LEFT, backButton, refreshButton, hiringButton, loadSampleButton, resetButton, suggestButton), BorderLayout.NORTH);
        body.add(centerPanel, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the four KPI cards shown at the top of the screen.
     */
    private JPanel buildMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 0));
        panel.setOpaque(false);
        panel.add(createMetricCard("Open Jobs", "Modules still accepting applications.", openJobsValue));
        panel.add(createMetricCard("Closed Jobs", "Modules already filled or paused.", closedJobsValue));
        panel.add(createMetricCard("Applications", "Total submissions in the system.", applicationCountValue));
        panel.add(createMetricCard("Accepted TAs", "Offers currently locked in.", acceptedCountValue));
        return panel;
    }

    /**
     * Builds the middle overview area containing directories and workload.
     */
    private JSplitPane buildOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.add(buildPeoplePanel(), BorderLayout.CENTER);
        panel.add(buildWorkloadPanel(), BorderLayout.SOUTH);
        return new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, new JPanel()) {
            {
                setEnabled(false);
                setDividerSize(0);
                setBorder(null);
                setTopComponent(panel);
                setBottomComponent(new JPanel());
                setResizeWeight(1.0);
            }
        };
    }

    /**
     * Builds the TA and MO directory split view.
     */
    private JSplitPane buildPeoplePanel() {
        JPanel taCard = UiTheme.createCard("TA Directory", "Scrollable TA profiles with core contact and skills information.");
        taCard.add(wrapDirectory(taDirectoryPanel), BorderLayout.CENTER);

        JPanel moCard = UiTheme.createCard("MO Directory", "Scrollable MO profiles with names and managed modules.");
        moCard.add(wrapDirectory(moDirectoryPanel), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, taCard, moCard);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        UiTheme.styleSplitPane(splitPane);
        return splitPane;
    }

    /**
     * Builds the workload card that highlights accepted teaching assignments.
     */
    private JPanel buildWorkloadPanel() {
        JPanel workloadCard = UiTheme.createCard("TA Workload", "Scrollable workload summary for accepted assignments.");
        JScrollPane tableScrollPane = UiTheme.wrapTable(workloadTable);
        tableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setPreferredSize(new Dimension(0, WORKLOAD_PANEL_HEIGHT));
        workloadCard.add(tableScrollPane, BorderLayout.CENTER);
        return workloadCard;
    }

    /**
     * Builds the lower half with job summaries, suggestions, and MO provisioning.
     */
    private JSplitPane buildBottomPanel() {
        JPanel jobsCard = UiTheme.createCard("Jobs and Assignments", "High-level list of current postings, hours, and statuses.");
        jobsCard.add(wrapArea(jobSummaryArea), BorderLayout.CENTER);

        JPanel suggestionsCard = UiTheme.createCard("Rebalance and MO Accounts", "Create MO accounts while reviewing staffing recommendations.");
        JPanel suggestionContent = new JPanel(new BorderLayout(0, 16));
        suggestionContent.setOpaque(false);
        suggestionContent.add(wrapArea(suggestionArea), BorderLayout.CENTER);
        suggestionContent.add(buildMoAccountPanel(), BorderLayout.SOUTH);
        suggestionsCard.add(suggestionContent, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                jobsCard,
                suggestionsCard
        );
        splitPane.setResizeWeight(0.5);
        UiTheme.styleSplitPane(splitPane);
        return splitPane;
    }

    /**
     * Reloads all repository-backed data and refreshes every admin widget.
     */
    private void refreshData() {
        workloadTableModel.setRowCount(0);
        List<User> users = dataService.getUserRepository().findAll();
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

        long openJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.OPEN).count();
        long closedJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.CLOSED).count();
        long acceptedApplications = applications.stream()
                .filter(application -> application.getStatus() == model.ApplicationStatus.ACCEPTED)
                .count();
        openJobsValue.setText(String.valueOf(openJobs));
        closedJobsValue.setText(String.valueOf(closedJobs));
        applicationCountValue.setText(String.valueOf(applications.size()));
        acceptedCountValue.setText(String.valueOf(acceptedApplications));
        refreshTaDirectory(profiles, applications);
        refreshMoDirectory(users);

        // The lower summary is intentionally plain text so it can double as a quick audit view in demos.
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

    /**
     * Rebuilds the TA directory list from profile and application data.
     */
    private void refreshTaDirectory(List<ApplicantProfile> profiles, List<ApplicationRecord> applications) {
        taDirectoryPanel.removeAll();
        List<JComponent> cards = new ArrayList<>();
        for (ApplicantProfile profile : profiles) {
            long submissionCount = applications.stream()
                    .filter(application -> profile.getApplicantId().equals(application.getApplicantId()))
                    .count();
            String skills = profile.getSkills().isEmpty() ? "-" : String.join(", ", profile.getSkills());
            String details = "Email: " + valueOrDash(profile.getEmail()) + "\n"
                    + "Phone: " + valueOrDash(profile.getPhone()) + "\n"
                    + "Skills: " + skills + "\n"
                    + "Availability: " + valueOrDash(profile.getAvailability()) + "\n"
                    + "Applications: " + submissionCount;
            cards.add(createDirectoryEntry(
                    valueOrDash(profile.getName()),
                    profile.getApplicantId(),
                    details,
                    "Delete TA",
                    () -> deleteTaAccount(profile)
            ));
        }
        installDirectoryEntries(taDirectoryPanel, cards, "No TA profiles are available yet.");
    }

    /**
     * Rebuilds the MO directory list from user data.
     */
    private void refreshMoDirectory(List<User> users) {
        moDirectoryPanel.removeAll();
        List<JComponent> cards = new ArrayList<>();
        for (User user : users) {
            if (user.getRole() != model.Role.MO) {
                continue;
            }
            String modules = user.getManagedModuleCodes().isEmpty()
                    ? "-"
                    : String.join(", ", user.getManagedModuleCodes());
            String details = "Email: " + valueOrDash(user.getUsername()) + "\n"
                    + "Modules: " + modules;
            cards.add(createDirectoryEntry(
                    valueOrDash(user.getName()),
                    user.getUserId(),
                    details,
                    "Delete MO",
                    () -> deleteMoAccount(user)
            ));
        }
        installDirectoryEntries(moDirectoryPanel, cards, "No MO accounts are available yet.");
    }

    /**
     * Builds the embedded form used to provision a new MO account.
     */
    private JPanel buildMoAccountPanel() {
        JPanel panel = UiTheme.createCard("Create MO Account", "Admin can provision an MO login and assign managed modules immediately.");

        UiTheme.styleTextField(moNameField);
        UiTheme.styleTextField(moEmailField);
        UiTheme.styleTextField(moPasswordField);
        UiTheme.styleTextField(moConfirmField);
        UiTheme.styleTextField(moModulesField);

        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "MO Name", moNameField);
        UiTheme.addFormRow(form, 2, "MO Email", moEmailField);
        UiTheme.addFormRow(form, 4, "Password", moPasswordField);
        UiTheme.addFormRow(form, 6, "Confirm Password", moConfirmField);
        UiTheme.addFormRow(form, 8, "Modules", moModulesField);

        JTextArea note = new JTextArea("Use commas to separate module codes, for example: COMP1001, DATA2002.");
        note.setEditable(false);
        note.setOpaque(false);
        note.setWrapStyleWord(true);
        note.setLineWrap(true);
        note.setForeground(UiTheme.MUTED_TEXT);
        note.setFont(UiTheme.uiFont(Font.PLAIN, 13));

        JButton clearButton = UiTheme.createSecondaryButton("Clear");
        JButton createButton = UiTheme.createPrimaryButton("Create MO Account");
        clearButton.addActionListener(event -> clearMoAccountForm());
        createButton.addActionListener(event -> createMoAccount());

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(form, BorderLayout.NORTH);
        body.add(note, BorderLayout.CENTER);
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, clearButton, createButton), BorderLayout.SOUTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Generates staffing suggestions for each currently open job.
     */
    private void generateSuggestions() {
        List<ApplicantProfile> profiles = dataService.getProfileRepository().findAll();
        List<JobPosting> jobs = dataService.getJobRepository().findAll();
        List<ApplicationRecord> applications = dataService.getApplicationRepository().findAll();
        int threshold = dataService.getConfig().getWorkloadThreshold();
        List<WorkloadRecord> workloads = workloadService.buildWorkloadRecords(profiles, jobs, applications, threshold);

        // Suggestions are limited to OPEN jobs because closed jobs already have enough accepted TAs.
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

    /**
     * Returns to the login frame and closes the admin dashboard.
     */
    private void returnToLogin() {
        new LoginFrame(dataService).setVisible(true);
        dispose();
    }

    /**
     * Opens the hiring-management frame used for job operations.
     */
    private void openHiringManagement() {
        new MOManagementFrame(dataService, currentUser).setVisible(true);
    }

    /**
     * Creates a new MO account from the values entered in the embedded form.
     */
    private void createMoAccount() {
        try {
            User user = authService.registerMo(
                    moEmailField.getText(),
                    moNameField.getText(),
                    new String(moPasswordField.getPassword()),
                    new String(moConfirmField.getPassword()),
                    moModules()
            );
            refreshData();
            clearMoAccountForm();
            UiMessage.info(this, "MO account created for " + user.getUsername() + ".");
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Parses the MO modules field into normalized module codes.
     */
    private List<String> moModules() {
        return validationService.parseSkills(moModulesField.getText()).stream()
                .map(validationService::normalizeModuleCode)
                .toList();
    }

    /**
     * Clears all fields in the MO account creation form.
     */
    private void clearMoAccountForm() {
        moNameField.setText("");
        moEmailField.setText("");
        moPasswordField.setText("");
        moConfirmField.setText("");
        moModulesField.setText("");
    }

    /**
     * Applies shared styling to tables and text areas used by the admin screen.
     */
    private void styleComponents() {
        UiTheme.styleTable(workloadTable);
        UiTheme.styleTextArea(jobSummaryArea, 14);
        UiTheme.styleTextArea(suggestionArea, 14);
        UiTheme.setColumnWidths(workloadTable, 180, 420, 120, 100);
        jobSummaryArea.setEditable(false);
        suggestionArea.setEditable(false);
    }

    /**
     * Wraps a directory panel in a themed scroll pane with vertical scrolling.
     */
    private JScrollPane wrapDirectory(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.setPreferredSize(new Dimension(0, DIRECTORY_PANEL_HEIGHT));
        scrollPane.setMinimumSize(new Dimension(220, DIRECTORY_PANEL_HEIGHT));
        UiTheme.styleScrollPane(scrollPane);
        return scrollPane;
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
     * Creates one metric card containing a heading and large numeric value.
     */
    private JPanel createMetricCard(String title, String subtitle, JLabel valueLabel) {
        JPanel card = UiTheme.createCard(title, subtitle);
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        content.add(valueLabel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates the shared container used by the TA and MO directories.
     */
    private static JPanel createDirectoryListPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * Creates one directory card with a right-aligned delete action.
     */
    private JComponent createDirectoryEntry(String title, String subtitle, String details, String actionText, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UiTheme.SURFACE_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(UiTheme.TEXT);
        titleLabel.setFont(UiTheme.uiFont(Font.BOLD, 15));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(UiTheme.MUTED_TEXT);
        subtitleLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JTextArea detailArea = new JTextArea(details);
        detailArea.setEditable(false);
        detailArea.setOpaque(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setForeground(UiTheme.TEXT);
        detailArea.setFont(UiTheme.uiFont(Font.PLAIN, 13));
        detailArea.setBorder(BorderFactory.createEmptyBorder());

        JButton deleteButton = UiTheme.createDangerButton(actionText);
        deleteButton.addActionListener(event -> action.run());
        deleteButton.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.add(Box.createVerticalGlue());
        actionPanel.add(deleteButton);
        actionPanel.add(Box.createVerticalGlue());

        header.add(titleLabel);
        header.add(subtitleLabel);
        content.add(header, BorderLayout.NORTH);
        content.add(detailArea, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.EAST);
        return card;
    }

    /**
     * Deletes a TA account and the related profile and application data.
     */
    private void deleteTaAccount(ApplicantProfile profile) {
        String name = valueOrDash(profile.getName());
        if (!UiMessage.confirm(this,
                "Are you sure you want to delete TA account \"" + name + "\"?\n"
                        + "This will remove the TA user, profile, and all related applications.",
                "Confirm TA Deletion")) {
            return;
        }

        try {
            List<ApplicationRecord> applications = new ArrayList<>(dataService.getApplicationRepository().findAll());
            applications.removeIf(application -> profile.getApplicantId().equals(application.getApplicantId()));
            dataService.getApplicationRepository().saveAll(applications);

            List<ApplicantProfile> profiles = new ArrayList<>(dataService.getProfileRepository().findAll());
            profiles.removeIf(existingProfile -> profile.getApplicantId().equals(existingProfile.getApplicantId()));
            dataService.getProfileRepository().saveAll(profiles);

            List<User> users = new ArrayList<>(dataService.getUserRepository().findAll());
            users.removeIf(user -> profile.getUserId().equals(user.getUserId()));
            dataService.getUserRepository().saveAll(users);

            refreshData();
            UiMessage.info(this, "TA account deleted successfully.");
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Deletes an MO account together with jobs and applications owned by that MO.
     */
    private void deleteMoAccount(User moUser) {
        String name = valueOrDash(moUser.getName());
        if (!UiMessage.confirm(this,
                "Are you sure you want to delete MO account \"" + name + "\"?\n"
                        + "This will remove the MO user, the jobs posted by this MO, and related applications.",
                "Confirm MO Deletion")) {
            return;
        }

        try {
            List<JobPosting> jobs = new ArrayList<>(dataService.getJobRepository().findAll());
            Set<String> deletedJobIds = new HashSet<>();
            jobs.removeIf(job -> {
                boolean shouldDelete = moUser.getUserId().equals(job.getPostedBy());
                if (shouldDelete) {
                    deletedJobIds.add(job.getJobId());
                }
                return shouldDelete;
            });
            dataService.getJobRepository().saveAll(jobs);

            List<ApplicationRecord> applications = new ArrayList<>(dataService.getApplicationRepository().findAll());
            applications.removeIf(application -> deletedJobIds.contains(application.getJobId()));
            dataService.getApplicationRepository().saveAll(applications);

            List<User> users = new ArrayList<>(dataService.getUserRepository().findAll());
            users.removeIf(user -> moUser.getUserId().equals(user.getUserId()));
            dataService.getUserRepository().saveAll(users);

            refreshData();
            UiMessage.info(this, "MO account deleted successfully.");
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Replaces directory contents with the supplied entries or an empty-state label.
     */
    private void installDirectoryEntries(JPanel panel, List<JComponent> entries, String emptyMessage) {
        panel.removeAll();
        if (entries.isEmpty()) {
            JLabel emptyLabel = new JLabel(emptyMessage);
            emptyLabel.setForeground(UiTheme.MUTED_TEXT);
            emptyLabel.setFont(UiTheme.uiFont(Font.PLAIN, 13));
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(emptyLabel);
        } else {
            for (JComponent entry : entries) {
                panel.add(entry);
                panel.add(Box.createVerticalStrut(10));
            }
        }
        panel.add(Box.createVerticalGlue());
        panel.revalidate();
        panel.repaint();
    }

    /**
     * Returns a dash for null or blank strings.
     */
    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * Creates the emphasized numeric label used inside metric cards.
     */
    private static JLabel createMetricValueLabel() {
        JLabel label = new JLabel("0");
        label.setFont(UiTheme.uiFont(Font.BOLD, 32));
        label.setForeground(UiTheme.TEXT);
        return label;
    }

    /**
     * Renderer that tints overloaded rows red for quick admin scanning.
     */
    private static class WorkloadRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // A single red-tinted row is enough to surface overload risk without introducing another status widget.
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
