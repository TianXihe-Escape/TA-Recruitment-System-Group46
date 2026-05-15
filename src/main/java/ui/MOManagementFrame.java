package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.JobCategory;
import model.JobPosting;
import model.JobStatus;
import model.Role;
import model.SkillMatchResult;
import model.User;
import model.WorkloadRecord;
import service.ApplicantService;
import service.ApplicationService;
import service.AuthService;
import service.CvStorageService;
import service.DataService;
import service.JobService;
import service.MatchingService;
import service.NotificationService;
import service.ValidationService;
import service.WorkloadService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MO dashboard for posting jobs and reviewing applicants.
 */
public class MOManagementFrame extends JFrame {
    private static final String NEW_JOB_PLACEHOLDER = "AUTO-GENERATED ON SAVE";
    private static final int VIEW_JOB_EDITOR = 0;
    private static final int VIEW_REVIEW_QUEUE = 1;
    private static final String[] VIEW_KEYS = {"job-editor", "review-queue"};

    private final DataService dataService;
    private final JobService jobService;
    private final AuthService authService;
    private final ApplicantService applicantService;
    private final ApplicationService applicationService;
    private final CvStorageService cvStorageService;
    private final NotificationService notificationService;
    private final WorkloadService workloadService;
    private final MatchingService matchingService;
    private final ValidationService validationService;
    private final User currentUser;
    private final List<String> managedModuleCodes;
    private final boolean adminMode;
    private boolean syncingForm;

    private final JTextField jobIdField = new JTextField();
    private final JComboBox<String> moduleCodeBox = new JComboBox<>();
    private final JTextField moduleTitleField = new JTextField();
    private final JComboBox<JobCategory> categoryBox = new JComboBox<>(JobCategory.values());
    private final JTextField semesterField = new JTextField();
    private final JTextField hoursField = new JTextField();
    private final JTextField requiredTaCountField = new JTextField();
    private final JTextField skillsField = new JTextField();
    private final JTextField deadlineField = new JTextField();
    private final JComboBox<JobStatus> statusBox = new JComboBox<>(JobStatus.values());
    private final JTextArea dutiesArea = new JTextArea(3, 20);
    private final JTextArea reviewArea = new JTextArea(4, 20);
    private final JTextArea applicantSummaryArea = new JTextArea(4, 20);
    private final JTextArea matchInfoArea = new JTextArea(6, 20);
    private final JComboBox<String> applicantStatusFilter = new JComboBox<>(
            new String[]{"All Statuses", "SUBMITTED", "SHORTLISTED", "ACCEPTED", "REJECTED", "WITHDRAWN"}
    );
    private final JComboBox<String> applicantSortBox = new JComboBox<>(
            new String[]{"Match % (High to Low)", "Match % (Low to High)", "Applicant Name (A-Z)", "Applicant Name (Z-A)", "Status"}
    );
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "Category", "Semester", "Hours", "TA Demand", "Deadline", "Status"}, 0);
    private final PlaceholderTable jobTable = new PlaceholderTable(jobTableModel, "No jobs are assigned to this MO yet.");
    private final DefaultTableModel applicantTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Applicant", "Status", "Match %", "Missing"}, 0);
    private final PlaceholderTable applicantTable = new PlaceholderTable(applicantTableModel, "Select a job and click Load Applicants to review submissions.");
    private final JPanel workspaceCards = new JPanel(new CardLayout());
    private final List<JToggleButton> navigationButtons = new ArrayList<>();
    private final AvatarButton avatarButton = new AvatarButton("MO");
    private final JLabel sidebarNameLabel = new JLabel();
    private final JLabel sidebarRoleLabel = new JLabel();
    private final JLabel workspaceTitleLabel = new JLabel();
    private final JLabel workspaceSubtitleLabel = new JLabel();
    private int currentWorkspaceView = VIEW_JOB_EDITOR;
    private String loadedApplicantJobId;
    private String selectedApplicantCvPath;
    private String selectedApplicantSupportingDocumentPath;

    public MOManagementFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;
        this.validationService = new ValidationService();
        this.cvStorageService = new CvStorageService();
        this.authService = new AuthService(
                dataService.getUserRepository(),
                dataService.getProfileRepository(),
                validationService
        );
        this.notificationService = new NotificationService(dataService.getNotificationRepository());
        this.workloadService = new WorkloadService();
        this.jobService = new JobService(dataService.getJobRepository(), validationService);
        this.applicantService = new ApplicantService(dataService.getProfileRepository(), validationService);
        this.matchingService = new MatchingService();
        this.applicationService = new ApplicationService(
                dataService.getApplicationRepository(),
                dataService.getJobRepository(),
                matchingService,
                new service.AllocationService(dataService.getAllocationRepository())
        );
        this.adminMode = currentUser.getRole() == Role.ADMIN;
        this.managedModuleCodes = resolveManagedModuleCodes();

        setTitle((adminMode ? "Hiring Management" : "MO Management") + " - " + Constants.APP_TITLE);
        setSize(1420, 880);
        setMinimumSize(new Dimension(1020, 700));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);
        styleComponents();
        avatarButton.addActionListener(event -> showAccountMenu());

        JPanel root = new JPanel(new BorderLayout(18, 0));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(UiTheme.wrapPage(buildWorkspacePanel()), BorderLayout.CENTER);
        add(root);

        refreshJobs();
        clearForm();
        bindFormSync();
        showWorkspace(VIEW_JOB_EDITOR);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 18));
        sidebar.setPreferredSize(new Dimension(226, 0));
        sidebar.setBackground(new Color(235, 242, 252));
        sidebar.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));

        JPanel accountPanel = new JPanel(new BorderLayout(10, 0));
        accountPanel.setOpaque(false);
        avatarButton.setInitials(initialsFor(displayName()));
        accountPanel.add(avatarButton, BorderLayout.WEST);

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        sidebarNameLabel.setText(displayName());
        sidebarNameLabel.setFont(UiTheme.uiFont(Font.BOLD, 15));
        sidebarNameLabel.setForeground(UiTheme.TEXT);
        sidebarRoleLabel.setText(adminMode ? "Admin Hiring" : "Module Organiser");
        sidebarRoleLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        sidebarRoleLabel.setForeground(UiTheme.MUTED_TEXT);
        identity.add(sidebarNameLabel);
        identity.add(Box.createVerticalStrut(4));
        identity.add(sidebarRoleLabel);
        accountPanel.add(identity, BorderLayout.CENTER);
        sidebar.add(accountPanel, BorderLayout.NORTH);

        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.add(createNavButton("Job Editor", SimpleLineIcon.Type.EDIT, VIEW_JOB_EDITOR));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavButton("Review Queue", SimpleLineIcon.Type.DOCUMENT, VIEW_REVIEW_QUEUE));
        sidebar.add(navigation, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildWorkspacePanel() {
        JPanel shell = new JPanel(new BorderLayout(0, 14));
        shell.setBackground(UiTheme.SURFACE);
        shell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        workspaceTitleLabel.setFont(UiTheme.uiFont(Font.BOLD, 22));
        workspaceTitleLabel.setForeground(UiTheme.TEXT);
        workspaceSubtitleLabel.setFont(UiTheme.uiFont(Font.PLAIN, 13));
        workspaceSubtitleLabel.setForeground(UiTheme.MUTED_TEXT);
        workspaceSubtitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        titlePanel.add(workspaceTitleLabel);
        titlePanel.add(workspaceSubtitleLabel);

        workspaceCards.setOpaque(false);
        workspaceCards.add(buildFormPanel(), VIEW_KEYS[VIEW_JOB_EDITOR]);
        workspaceCards.add(buildTablesPanel(), VIEW_KEYS[VIEW_REVIEW_QUEUE]);

        shell.add(titlePanel, BorderLayout.NORTH);
        shell.add(workspaceCards, BorderLayout.CENTER);
        return shell;
    }

    private JToggleButton createNavButton(String text, SimpleLineIcon.Type iconType, int viewIndex) {
        JToggleButton button = new JToggleButton(text);
        button.setIcon(new SimpleLineIcon(iconType, UiTheme.MUTED_TEXT));
        button.setIconTextGap(10);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(UiTheme.uiFont(Font.BOLD, 14));
        button.setForeground(UiTheme.TEXT);
        button.setBackground(new Color(235, 242, 252));
        button.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.addActionListener(event -> showWorkspace(viewIndex));
        navigationButtons.add(button);
        return button;
    }

    private void showWorkspace(int viewIndex) {
        currentWorkspaceView = viewIndex;
        ((CardLayout) workspaceCards.getLayout()).show(workspaceCards, VIEW_KEYS[viewIndex]);
        for (int i = 0; i < navigationButtons.size(); i++) {
            JToggleButton button = navigationButtons.get(i);
            boolean selected = i == viewIndex;
            button.setSelected(selected);
            button.setBackground(selected ? Color.WHITE : new Color(235, 242, 252));
            button.setForeground(selected ? UiTheme.PRIMARY : UiTheme.TEXT);
        }
        if (viewIndex == VIEW_JOB_EDITOR) {
            workspaceTitleLabel.setText(adminMode ? "Admin Hiring Console" : "Job Posting Editor");
            workspaceSubtitleLabel.setText(buildScopeSummary());
        } else {
            workspaceTitleLabel.setText("Review Queue");
            workspaceSubtitleLabel.setText("Inspect postings, load applicants, review match details, and update hiring decisions.");
        }
    }

    private void showAccountMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        menu.add(buildAccountHeader());
        menu.addSeparator();
        menu.add(menuItem("Job Editor", SimpleLineIcon.Type.EDIT, () -> showWorkspace(VIEW_JOB_EDITOR)));
        menu.add(menuItem("Review Queue", SimpleLineIcon.Type.DOCUMENT, () -> showWorkspace(VIEW_REVIEW_QUEUE)));
        menu.add(menuItem("Change Password", SimpleLineIcon.Type.SAVE, this::showChangePasswordDialog));
        menu.add(menuItem("View Notifications", SimpleLineIcon.Type.BELL, this::showNotifications));
        menu.add(menuItem("Refresh", SimpleLineIcon.Type.REFRESH, this::refreshWorkspace));
        menu.addSeparator();
        menu.add(menuItem(adminMode ? "Back to Admin" : "Logout", SimpleLineIcon.Type.LOGOUT, this::goBack));
        menu.show(avatarButton, 0, avatarButton.getHeight() + 6);
    }

    private JPanel buildAccountHeader() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(UiTheme.SURFACE);
        AvatarButton preview = new AvatarButton(initialsFor(displayName()));
        preview.setEnabled(false);
        panel.add(preview, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(displayName());
        name.setFont(UiTheme.uiFont(Font.BOLD, 16));
        name.setForeground(UiTheme.TEXT);
        JLabel username = new JLabel(currentUser.getUsername());
        username.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        username.setForeground(UiTheme.MUTED_TEXT);
        JLabel role = new JLabel(adminMode ? "Admin access" : "Modules: " + String.join(", ", managedModuleCodes));
        role.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        role.setForeground(UiTheme.MUTED_TEXT);
        text.add(name);
        text.add(Box.createVerticalStrut(4));
        text.add(username);
        text.add(Box.createVerticalStrut(3));
        text.add(role);
        panel.add(text, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(340, 74));
        return panel;
    }

    private JMenuItem menuItem(String text, SimpleLineIcon.Type iconType, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setIcon(new SimpleLineIcon(iconType, UiTheme.MUTED_TEXT));
        item.setFont(UiTheme.uiFont(Font.PLAIN, 14));
        item.setForeground(UiTheme.TEXT);
        item.setIconTextGap(10);
        item.setBorder(BorderFactory.createEmptyBorder(9, 8, 9, 8));
        item.addActionListener(event -> action.run());
        return item;
    }

    private void decorateButton(AbstractButton button, SimpleLineIcon.Type iconType) {
        button.setIcon(new SimpleLineIcon(iconType, Color.WHITE));
        button.setIconTextGap(8);
    }

    private void showChangePasswordDialog() {
        new ChangePasswordFrame(authService, currentUser).setVisible(true);
    }

    private void refreshWorkspace() {
        refreshJobs();
        if (loadedApplicantJobId != null) {
            loadApplicantsForJob(loadedApplicantJobId);
        }
    }

    private String displayName() {
        if (currentUser.getName() != null && !currentUser.getName().isBlank()) {
            return currentUser.getName();
        }
        if (currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) {
            return currentUser.getUsername();
        }
        return adminMode ? "Admin" : "MO";
    }

    private String initialsFor(String value) {
        if (value == null || value.isBlank()) {
            return adminMode ? "AD" : "MO";
        }
        String trimmed = value.trim();
        if (trimmed.contains(" ")) {
            String[] parts = trimmed.split("\\s+");
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                if (!part.isBlank()) {
                    builder.append(part.charAt(0));
                }
                if (builder.length() == 2) {
                    break;
                }
            }
            return builder.isEmpty() ? "MO" : builder.toString();
        }
        return trimmed.length() <= 2 ? trimmed : trimmed.substring(0, 2);
    }

    private JPanel buildFormPanel() {
        JPanel panel = UiTheme.createCard(null, null);

        JPanel form = UiTheme.createFormGrid();
        jobIdField.setEditable(false);
        jobIdField.setForeground(Color.GRAY);
        matchInfoArea.setEditable(false);
        populateManagedModules();
        UiTheme.addFormRow(form, 0, "Job ID", jobIdField);
        UiTheme.addFormRow(form, 2, "Module Code", moduleCodeBox);
        UiTheme.addFormRow(form, 4, "Module Title", moduleTitleField);
        UiTheme.addFormRow(form, 6, "Category", categoryBox);
        UiTheme.addFormRow(form, 8, "Semester", semesterField);
        UiTheme.addFormRow(form, 10, "Hours", hoursField);
        UiTheme.addFormRow(form, 12, "TA Needed", requiredTaCountField);
        UiTheme.addFormRow(form, 14, "Required Skills", skillsField);
        UiTheme.addFormRow(form, 16, "Deadline (YYYY-MM-DD)", deadlineField);
        UiTheme.addFormRow(form, 18, "Status", statusBox);
        UiTheme.addFormRow(form, 20, "Duties", wrapArea(dutiesArea));

        JPanel lower = UiTheme.createCard("Applicant Match Details", "Review fit, missing skills, and applicant notes for the selected submission.");
        lower.add(wrapArea(matchInfoArea), BorderLayout.CENTER);

        JButton backButton = adminMode
                ? UiTheme.createDangerButton("Back to Previous Page")
                : UiTheme.createSecondaryButton("Back to Login");
        JButton newButton = UiTheme.createSecondaryButton("New Job");
        JButton saveButton = UiTheme.createPrimaryButton("Save Job");
        decorateButton(backButton, SimpleLineIcon.Type.LOGOUT);
        decorateButton(newButton, SimpleLineIcon.Type.EDIT);
        decorateButton(saveButton, SimpleLineIcon.Type.SAVE);

        boolean hasManagedModules = !managedModuleCodes.isEmpty();
        newButton.setEnabled(hasManagedModules);
        saveButton.setEnabled(hasManagedModules);
        moduleCodeBox.setEnabled(hasManagedModules);

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

    private JPanel buildTablesPanel() {
        JPanel panel = UiTheme.createCard(null, null);

        JButton loadApplicantsButton = UiTheme.createSecondaryButton("Load Applicants");
        JButton shortlistButton = UiTheme.createSecondaryButton("Shortlist");
        JButton removeShortlistButton = UiTheme.createSecondaryButton("Remove Shortlist");
        JButton acceptButton = UiTheme.createPrimaryButton("Accept");
        JButton cancelAcceptanceButton = UiTheme.createSecondaryButton("Cancel Acceptance");
        JButton rejectButton = UiTheme.createDangerButton("Reject");
        JButton refreshButton = UiTheme.createSecondaryButton("Refresh");
        JButton notificationsButton = UiTheme.createSecondaryButton("View Notifications");
        decorateButton(loadApplicantsButton, SimpleLineIcon.Type.DOCUMENT);
        decorateButton(shortlistButton, SimpleLineIcon.Type.STAR);
        decorateButton(removeShortlistButton, SimpleLineIcon.Type.LOGOUT);
        decorateButton(acceptButton, SimpleLineIcon.Type.CHECK);
        decorateButton(cancelAcceptanceButton, SimpleLineIcon.Type.REFRESH);
        decorateButton(rejectButton, SimpleLineIcon.Type.TRASH);
        decorateButton(refreshButton, SimpleLineIcon.Type.REFRESH);
        decorateButton(notificationsButton, SimpleLineIcon.Type.BELL);

        loadApplicantsButton.addActionListener(event -> loadApplicantsForSelectedJob());
        shortlistButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.SHORTLISTED));
        removeShortlistButton.addActionListener(event -> removeShortlist());
        acceptButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.ACCEPTED));
        cancelAcceptanceButton.addActionListener(event -> cancelAcceptedApplication());
        rejectButton.addActionListener(event -> updateApplicationStatus(ApplicationStatus.REJECTED));
        notificationsButton.addActionListener(event -> showNotifications());
        refreshButton.addActionListener(event -> {
            refreshJobs();
            applicantTableModel.setRowCount(0);
            reviewArea.setText("");
            applicantSummaryArea.setText("");
            matchInfoArea.setText("");
            clearSelectedApplicantCv();
            loadedApplicantJobId = null;
            updateApplicantEmptyState();
        });
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

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(sortLabel);
        filterPanel.add(applicantSortBox);
        filterPanel.add(filterLabel);
        filterPanel.add(applicantStatusFilter);

        JPanel topControls = new JPanel();
        topControls.setOpaque(false);
        topControls.setLayout(new BoxLayout(topControls, BoxLayout.Y_AXIS));
        topControls.add(UiTheme.createButtonRow(FlowLayout.LEFT, loadApplicantsButton, shortlistButton, removeShortlistButton, acceptButton, cancelAcceptanceButton, rejectButton, notificationsButton, refreshButton));
        topControls.add(Box.createVerticalStrut(8));
        topControls.add(filterPanel);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(topControls, BorderLayout.NORTH);
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
                    job.getCategory() == null ? "-" : job.getCategory().getDisplayName(),
                    valueOrDash(job.getSemester()),
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
        categoryBox.setSelectedItem(JobCategory.MODULE_TA);
        semesterField.setText("");
        hoursField.setText("");
        requiredTaCountField.setText("1");
        skillsField.setText("");
        deadlineField.setText("");
        dutiesArea.setText("");
        statusBox.setSelectedItem(JobStatus.OPEN);
        matchInfoArea.setText("");
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        clearSelectedApplicantCv();
        applicantTableModel.setRowCount(0);
        loadedApplicantJobId = null;
        updateApplicantEmptyState();
        moduleCodeBox.requestFocusInWindow();
        syncingForm = false;
    }

    private void saveJob() {
        try {
            if (managedModuleCodes.isEmpty()) {
                UiMessage.error(this, adminMode
                        ? "No manageable module codes are available yet."
                        : "This MO is not assigned to any modules yet.");
                return;
            }
            String jobId = jobIdField.getText().trim();
            JobPosting existingJob = NEW_JOB_PLACEHOLDER.equals(jobId) || jobId.isBlank()
                    ? null
                    : jobService.getJobById(jobId);
            String moduleCode = String.valueOf(moduleCodeBox.getSelectedItem()).trim();
            if (!canManageModule(moduleCode)) {
                throw new IllegalArgumentException("You can only manage TA hiring for your assigned modules.");
            }
            if (existingJob != null && !canManageModule(existingJob.getModuleCode())) {
                throw new IllegalArgumentException("You can only edit jobs for your assigned modules.");
            }
            List<String> skillErrors = validationService.validateSkillInput(skillsField.getText(), "Required skills", true);
            if (!skillErrors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\n", skillErrors));
            }

            JobPosting jobPosting = new JobPosting();
            jobPosting.setJobId(NEW_JOB_PLACEHOLDER.equals(jobId) ? "" : jobId);
            jobPosting.setModuleCode(moduleCode);
            jobPosting.setModuleTitle(moduleTitleField.getText().trim());
            jobPosting.setCategory((JobCategory) categoryBox.getSelectedItem());
            jobPosting.setSemester(semesterField.getText().trim());
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
            UiMessage.error(this, "Deadline must use YYYY-MM-DD format, for example 2026-04-30.");
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
            clearSelectedApplicantCv();
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(applicationRow, 0));
        String jobId = String.valueOf(jobTableModel.getValueAt(jobRow, 0));
        ApplicationRecord application = applicationService.getApplicationsForJob(jobId).stream()
                .filter(item -> item.getApplicationId().equals(applicationId))
                .findFirst()
                .orElse(null);
        if (application == null) {
            clearSelectedApplicantCv();
            return;
        }
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        JobPosting job = findJob(jobId).orElse(null);
        if (applicant == null || job == null) {
            matchInfoArea.setText("The selected applicant or job record no longer exists. Refresh the review queue.");
            applicantSummaryArea.setText("");
            reviewArea.setText("");
            clearSelectedApplicantCv();
            return;
        }
        SkillMatchResult matchResult = matchingService.calculateMatch(applicant.getSkills(), job.getRequiredSkills());
        setSelectedApplicantDocuments(applicant.getCvPath(), applicant.getSupportingDocumentPath());
        matchInfoArea.setText(
                "Applicant: " + applicant.getName() + "\n" +
                        "Skills: " + String.join(", ", applicant.getSkills()) + "\n" +
                        "CV: " + applicant.getCvPath() + "\n" +
                        "Supporting Document: " + valueOrDash(applicant.getSupportingDocumentPath()) + "\n" +
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
        if ((status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED)
                && !UiMessage.confirm(this, "Are you sure you want to mark this application as " + status + "?", "Confirm Review Action")) {
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(row, 0));
        int jobRow = jobTable.getSelectedRow();
        String selectedJobId = jobRow >= 0 ? String.valueOf(jobTableModel.getValueAt(jobRow, 0)) : null;
        if (status == ApplicationStatus.ACCEPTED && !confirmWorkloadBeforeAccept(applicationId)) {
            return;
        }
        try {
            applicationService.updateStatus(applicationId, status, reviewArea.getText().trim(), currentUser.getUserId());
            notifyStatusChange(applicationId, status);
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

    private void removeShortlist() {
        int row = applicantTable.getSelectedRow();
        if (row < 0) {
            UiMessage.error(this, "Please select an applicant first.");
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(row, 0));
        int jobRow = jobTable.getSelectedRow();
        String selectedJobId = jobRow >= 0 ? String.valueOf(jobTableModel.getValueAt(jobRow, 0)) : null;
        try {
            applicationService.removeShortlist(applicationId, reviewArea.getText().trim(), currentUser.getUserId());
            notifyStatusChange(applicationId, ApplicationStatus.SUBMITTED);
            UiMessage.info(this, "Shortlist status removed.");
            if (selectedJobId != null) {
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
        if (!UiMessage.confirm(this, "Are you sure you want to cancel this accepted application and reopen the slot?", "Confirm Cancellation")) {
            return;
        }
        String applicationId = String.valueOf(applicantTableModel.getValueAt(row, 0));
        int jobRow = jobTable.getSelectedRow();
        String selectedJobId = jobRow >= 0 ? String.valueOf(jobTableModel.getValueAt(jobRow, 0)) : null;
        try {
            applicationService.cancelAcceptance(applicationId, reviewArea.getText().trim(), currentUser.getUserId());
            notifyStatusChange(applicationId, ApplicationStatus.SHORTLISTED);
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

    private boolean confirmWorkloadBeforeAccept(String applicationId) {
        ApplicationRecord application = dataService.getApplicationRepository().findById(applicationId).orElse(null);
        if (application == null) {
            return true;
        }
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        JobPosting job = findJob(application.getJobId()).orElse(null);
        if (applicant == null || job == null) {
            return true;
        }

        int threshold = dataService.getConfig().getWorkloadThreshold();
        List<WorkloadRecord> workloads = workloadService.buildWorkloadRecords(
                dataService.getProfileRepository().findAll(),
                dataService.getJobRepository().findAll(),
                dataService.getApplicationRepository().findAll(),
                threshold
        );
        int projectedHours = workloadService.projectedHours(applicant.getApplicantId(), job, workloads);
        if (projectedHours <= threshold) {
            return true;
        }

        return UiMessage.confirm(this,
                applicant.getName() + " would reach " + projectedHours + "h after this acceptance, above the "
                        + threshold + "h workload threshold. Continue?",
                "Workload Warning");
    }

    private void notifyStatusChange(String applicationId, ApplicationStatus status) {
        ApplicationRecord application = dataService.getApplicationRepository().findById(applicationId).orElse(null);
        if (application == null) {
            return;
        }
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        JobPosting job = findJob(application.getJobId()).orElse(null);
        if (job == null) {
            return;
        }
        String jobName = job.getModuleCode() + " " + job.getModuleTitle();
        if (applicant != null) {
            notificationService.notifyUser(applicant.getUserId(), "Your application for " + jobName + " is now " + status + ".");
        }
        notificationService.notifyUser(currentUser.getUserId(), "Application " + applicationId + " for " + jobName + " updated to " + status + ".");
    }

    private void showNotifications() {
        StringBuilder builder = new StringBuilder();
        for (model.NotificationRecord notification : notificationService.getNotificationsForUser(currentUser.getUserId())) {
            builder.append(notification.getCreatedAt() == null ? "-" : notification.getCreatedAt())
                    .append(" | ")
                    .append(notification.isRead() ? "Read" : "New")
                    .append("\n")
                    .append(notification.getMessage())
                    .append("\n\n");
        }
        if (builder.isEmpty()) {
            builder.append("No notifications yet.");
        }
        notificationService.markAllRead(currentUser.getUserId());
        UiMessage.info(this, builder.toString());
    }

    private void loadApplicantsForJob(String jobId) {
        JobPosting job = findJob(jobId).orElse(null);
        if (job == null) {
            loadedApplicantJobId = null;
            applicantTableModel.setRowCount(0);
            reviewArea.setText("");
            applicantSummaryArea.setText("");
            matchInfoArea.setText("");
            clearSelectedApplicantCv();
            updateApplicantEmptyState();
            UiMessage.error(this, "This job no longer exists. Please refresh the job list.");
            refreshJobs();
            return;
        }
        if (!canManageModule(job.getModuleCode())) {
            UiMessage.error(this, "You can only review applicants for your assigned modules.");
            return;
        }
        loadedApplicantJobId = jobId;
        applicantTableModel.setRowCount(0);
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        matchInfoArea.setText("");
        clearSelectedApplicantCv();
        List<ApplicationRecord> applications = applicationService.getApplicationsForJob(jobId).stream()
                .filter(this::matchesApplicantFilter)
                .sorted(this::compareApplications)
                .toList();
        for (ApplicationRecord application : applications) {
            ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
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
                "Programme: " + valueOrDash(applicant.getProgramme()) + "\n" +
                "Year of Study: " + valueOrDash(applicant.getYearOfStudy()) + "\n" +
                "Availability: " + valueOrDash(applicant.getAvailability()) + "\n" +
                "Preferred Duties: " + valueOrDash(applicant.getPreferredDuties()) + "\n" +
                "Application Status: " + application.getStatus() + "\n" +
                "Last Updated: " + valueOrDash(application.getLastUpdatedAt() == null ? null : application.getLastUpdatedAt().toString()) + "\n" +
                "Decision At: " + valueOrDash(application.getDecisionAt() == null ? null : application.getDecisionAt().toString()) + "\n" +
                "Match Score: " + application.getMatchScore() + "%\n" +
                "Missing Skills: " + valueOrDash(String.join(", ", application.getMissingSkills())) + "\n" +
                "CV Path: " + valueOrDash(applicant.getCvPath()) + "\n" +
                "Supporting Document: " + valueOrDash(applicant.getSupportingDocumentPath()) + "\n" +
                "For Job: " + job.getModuleCode() + " - " + job.getModuleTitle() + "\n\n" +
                "Experience Summary:\n" + valueOrDash(applicant.getExperienceSummary());
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void setSelectedApplicantDocuments(String cvPath, String supportingDocumentPath) {
        selectedApplicantCvPath = cvPath == null ? "" : cvPath.trim();
        selectedApplicantSupportingDocumentPath = supportingDocumentPath == null ? "" : supportingDocumentPath.trim();
        boolean hasDocumentPath = !selectedApplicantCvPath.isBlank() || !selectedApplicantSupportingDocumentPath.isBlank();
        matchInfoArea.setCursor(Cursor.getPredefinedCursor(hasDocumentPath ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        matchInfoArea.setToolTipText(hasDocumentPath
                ? "Click the CV or supporting document line to open the file."
                : "The selected applicant has not uploaded documents.");
    }

    private void clearSelectedApplicantCv() {
        selectedApplicantCvPath = "";
        selectedApplicantSupportingDocumentPath = "";
        matchInfoArea.setCursor(Cursor.getDefaultCursor());
        matchInfoArea.setToolTipText("Select an applicant to view CV details.");
    }

    private void openSelectedApplicantCv() {
        openApplicantDocument(selectedApplicantCvPath, "CV file");
    }

    private void openSelectedApplicantSupportingDocument() {
        openApplicantDocument(selectedApplicantSupportingDocumentPath, "supporting document");
    }

    private void openApplicantDocument(String documentPath, String label) {
        if (documentPath == null || documentPath.isBlank()) {
            UiMessage.error(this, "No " + label + " is available for the selected applicant.");
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

    private void openCvFromMatchInfoClick(MouseEvent event) {
        if ((selectedApplicantCvPath == null || selectedApplicantCvPath.isBlank())
                && (selectedApplicantSupportingDocumentPath == null || selectedApplicantSupportingDocumentPath.isBlank())) {
            return;
        }

        int offset = matchInfoArea.viewToModel2D(event.getPoint());
        try {
            int line = matchInfoArea.getLineOfOffset(offset);
            int lineStart = matchInfoArea.getLineStartOffset(line);
            int lineEnd = matchInfoArea.getLineEndOffset(line);
            String clickedLine = matchInfoArea.getText(lineStart, lineEnd - lineStart).trim();
            if (clickedLine.startsWith("CV:")) {
                openSelectedApplicantCv();
            } else if (clickedLine.startsWith("Supporting Document:")) {
                openSelectedApplicantSupportingDocument();
            }
        } catch (Exception ex) {
            UiMessage.error(this, "Could not read the selected CV line:\n" + ex.getMessage());
        }
    }

    private void styleComponents() {
        UiTheme.styleTextField(jobIdField);
        UiTheme.styleComboBox(moduleCodeBox);
        UiTheme.styleTextField(moduleTitleField);
        UiTheme.styleComboBox(categoryBox);
        UiTheme.styleTextField(semesterField);
        UiTheme.styleTextField(hoursField);
        UiTheme.styleTextField(requiredTaCountField);
        UiTheme.styleTextField(skillsField);
        UiTheme.styleTextField(deadlineField);
        UiTheme.styleComboBox(statusBox);
        UiTheme.styleTextArea(dutiesArea, 5);
        UiTheme.styleTextArea(reviewArea, 4);
        UiTheme.styleTextArea(applicantSummaryArea, 8);
        UiTheme.styleTextArea(matchInfoArea, 8);
        clearSelectedApplicantCv();
        matchInfoArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                openCvFromMatchInfoClick(event);
            }
        });
        UiTheme.styleComboBox(applicantStatusFilter);
        UiTheme.styleComboBox(applicantSortBox);
        UiTheme.styleTable(jobTable);
        UiTheme.styleTable(applicantTable);
        jobTable.getColumnModel().getColumn(6).setCellRenderer(new DeadlineWarningRenderer());
        jobTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());
        applicantTable.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer());
        UiTheme.setColumnWidths(jobTable, 100, 280, 120, 110, 70, 110, 140, 100);
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
                .filter(job -> canManageModule(job.getModuleCode()))
                .collect(Collectors.toList());
    }

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

    private void populateManagedModules() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String moduleCode : managedModuleCodes) {
            model.addElement(moduleCode);
        }
        moduleCodeBox.setModel(model);
    }

    private boolean canManageModule(String moduleCode) {
        return adminMode || currentUser.managesModule(moduleCode);
    }

    private List<String> resolveManagedModuleCodes() {
        if (!adminMode) {
            return currentUser.getManagedModuleCodes();
        }

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
        categoryBox.setSelectedItem(JobCategory.MODULE_TA);
        semesterField.setText("");
        reviewArea.setText("");
        applicantSummaryArea.setText("");
        matchInfoArea.setText("");
        clearSelectedApplicantCv();
        applicantTableModel.setRowCount(0);
        loadedApplicantJobId = null;
        updateApplicantEmptyState();
        syncingForm = false;
    }

    private void applyJobToForm(JobPosting job) {
        syncingForm = true;
        jobIdField.setForeground(Color.BLACK);
        jobIdField.setText(job.getJobId());
        moduleCodeBox.setSelectedItem(job.getModuleCode());
        moduleTitleField.setText(job.getModuleTitle());
        categoryBox.setSelectedItem(job.getCategory());
        semesterField.setText(job.getSemester());
        hoursField.setText(String.valueOf(job.getHours()));
        requiredTaCountField.setText(String.valueOf(job.getRequiredTaCount()));
        skillsField.setText(String.join(", ", job.getRequiredSkills()));
        deadlineField.setText(String.valueOf(job.getApplicationDeadline()));
        statusBox.setSelectedItem(job.getStatus());
        dutiesArea.setText(job.getDuties());
        matchInfoArea.setText("");
        applicantSummaryArea.setText("");
        reviewArea.setText("");
        clearSelectedApplicantCv();
        applicantTableModel.setRowCount(0);
        loadedApplicantJobId = null;
        updateApplicantEmptyState();
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
        // Reopening only needs enough accepted TAs removed to create at least one open slot again.
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
            applicationService.cancelAcceptance(application.getApplicationId(), reviewerNotes, currentUser.getUserId());
            notifyStatusChange(application.getApplicationId(), ApplicationStatus.SHORTLISTED);
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
        // Closing fills only the remaining vacancies; already accepted TAs stay accepted.
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
                if (!confirmWorkloadBeforeAccept(application.getApplicationId())) {
                    return false;
                }
                applicationService.updateStatus(application.getApplicationId(), ApplicationStatus.ACCEPTED, reviewerNotes, currentUser.getUserId());
                notifyStatusChange(application.getApplicationId(), ApplicationStatus.ACCEPTED);
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
                .filter(application -> application.getStatus() != ApplicationStatus.WITHDRAWN)
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
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        String applicantName = applicant == null ? "[Deleted Applicant]" : valueOrDash(applicant.getName());
        String applicantEmail = applicant == null ? "-" : valueOrDash(applicant.getEmail());
        String label = applicantName
                + " | " + applicantEmail
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

    private boolean matchesApplicantFilter(ApplicationRecord application) {
        String selected = String.valueOf(applicantStatusFilter.getSelectedItem());
        return "All Statuses".equals(selected) || application.getStatus().name().equals(selected);
    }

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

    private String applicantNameFor(ApplicationRecord application) {
        ApplicantProfile applicant = findApplicant(application.getApplicantId()).orElse(null);
        if (applicant == null || applicant.getName() == null) {
            return "";
        }
        return applicant.getName();
    }

    private Optional<JobPosting> findJob(String jobId) {
        return dataService.getJobRepository().findById(jobId);
    }

    private Optional<ApplicantProfile> findApplicant(String applicantId) {
        return dataService.getProfileRepository().findByApplicantId(applicantId);
    }

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
