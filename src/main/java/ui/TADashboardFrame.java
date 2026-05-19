package ui;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.JobCategory;
import model.JobPosting;
import model.MessageRecord;
import model.NotificationRecord;
import model.User;
import service.AuthService;
import service.AccountCleanupService;
import service.ApplicantService;
import service.ApplicationService;
import service.CvStorageService;
import service.DataService;
import service.JobService;
import service.MatchingService;
import service.MessageService;
import service.NotificationService;
import service.ValidationService;
import ui.dialogs.ApplicationDetailsDialog;
import ui.dialogs.JobDetailsDialog;
import ui.dialogs.MessageDetailsDialog;
import ui.dialogs.NotificationDetailsDialog;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main TA workspace.
 * This frame combines profile editing, job browsing, and application tracking
 * so applicants can complete their full workflow from one screen.
 */
public class TADashboardFrame extends JFrame {
    // File chooser text is centralized so the same wording appears anywhere the
    // applicant is asked to upload or replace a CV.
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
     * Authentication service used for password changes and account actions.
     */
    private final AuthService authService;
    /**
     * Service for querying jobs.
     */
    private final JobService jobService;
    /**
     * Service for submission and application workflow actions.
     */
    private final ApplicationService applicationService;
    /**
     * Notification helper for unread badges and applicant-facing alerts.
     */
    private final NotificationService notificationService;
    /**
     * Message helper for TA/MO conversation threads.
     */
    private final MessageService messageService;
    private final MatchingService matchingService;
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
    // Profile-editor controls reused whenever the TA opens the modal edit dialog.
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
    private final JButton chooseSupportingDocumentButton = UiTheme.createSecondaryButton("Choose Files");
    // Shared filter controls for the opportunities and favourites workspaces.
    private final JTextField jobSearchField = new JTextField();
    private final JComboBox<String> moduleFilterBox = new JComboBox<>();
    private final JComboBox<String> categoryFilterBox = new JComboBox<>();
    private boolean syncingJobFilters;
    // Table models back the five card-based TA workspace views.
    private final DefaultTableModel jobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "MO", "Job Type", "Schedule", "Location", "Workload", "TA Demand", "Deadline", "Skills", "Match Score", "Favourite", "Status"}, 0);
    private final JTable jobTable = new PlaceholderTable(jobTableModel, "No open jobs are available right now.");
    private final DefaultTableModel favoriteJobTableModel = new DefaultTableModel(
            new Object[]{"Job ID", "Module", "MO", "Job Type", "Schedule", "Location", "Workload", "TA Demand", "Deadline", "Skills", "Match Score", "Favourite", "Status"}, 0);
    private final JTable favoriteJobTable = new PlaceholderTable(favoriteJobTableModel, "No favourite jobs match the current filters.");
    private final DefaultTableModel applicationTableModel = new DefaultTableModel(
            new Object[]{"Application ID", "Job ID", "Status", "Match %", "Missing Skills", "Reviewer Notes"}, 0);
    private final JTable applicationTable = new PlaceholderTable(applicationTableModel, "You have not submitted any applications yet.");
    private final DefaultTableModel notificationTableModel = new DefaultTableModel(
            new Object[]{"Notification ID", "Time", "Status", "Message"}, 0);
    private final JTable notificationTable = new PlaceholderTable(notificationTableModel, "No notifications yet.");
    private final DefaultTableModel messageTableModel = new DefaultTableModel(
            new Object[]{"Message ID", "Time", "Job", "Direction", "Status", "Message"}, 0);
    private final JTable messageTable = new PlaceholderTable(messageTableModel, "No TA/MO messages yet.");
    private final JLabel notificationStatusLabel = new JLabel();
    private final JLabel messageStatusLabel = new JLabel();
    private JPanel workspaceShell;
    // CardLayout view indexes used by the left navigation rail.
    private static final int VIEW_AVAILABLE_JOBS = 0;
    private static final int VIEW_FAVOURITE_JOBS = 1;
    private static final int VIEW_APPLICATIONS = 2;
    private static final int VIEW_NOTIFICATIONS = 3;
    private static final int VIEW_MESSAGES = 4;
    private static final String[] VIEW_KEYS = {"available", "favourites", "applications", "notifications", "messages"};
    private final JPanel workspaceCards = new JPanel(new CardLayout());
    private final JPanel contextualActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final JPanel jobFilterContainer = new JPanel(new BorderLayout());
    private final JLabel workspaceTitleLabel = new JLabel();
    private final JLabel workspaceSubtitleLabel = new JLabel();
    private final JLabel sidebarNameLabel = new JLabel();
    private final JLabel sidebarRoleLabel = new JLabel("TA Applicant");
    private final List<JToggleButton> navigationButtons = new ArrayList<>();
    private final AvatarButton avatarButton = new AvatarButton("TA");
    private int currentWorkspaceView = VIEW_AVAILABLE_JOBS;
    // Contextual actions are rebuilt per workspace so the top action strip stays focused.
    private final JButton viewDetailsButton = createIconButton("View Job Details", SimpleLineIcon.Type.EYE, false);
    private final JButton viewApplicationButton = createIconButton("View Application Details", SimpleLineIcon.Type.DOCUMENT, false);
    private final JButton withdrawButton = createIconButton("Withdraw Application", SimpleLineIcon.Type.LOGOUT, false);
    private final JButton favouriteButton = createIconButton("Toggle Favourite", SimpleLineIcon.Type.STAR, false);
    private final JButton applyButton = createIconButton("Apply", SimpleLineIcon.Type.SEND, true);
    private final JButton markReadButton = createIconButton("Mark Notifications Read", SimpleLineIcon.Type.CHECK, false);
    private final JButton messageMoButton = createIconButton("Message MO", SimpleLineIcon.Type.SEND, false);
    private final JButton replyMessageButton = createIconButton("Reply", SimpleLineIcon.Type.SEND, false);
    private final JButton markMessagesReadButton = createIconButton("Mark Messages Read", SimpleLineIcon.Type.CHECK, false);

    /**
     * Constructs the TA dashboard and loads the initial data snapshot.
     */
    public TADashboardFrame(DataService dataService, User currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;
        this.validationService = new ValidationService();
        this.cvStorageService = new CvStorageService();
        this.authService = new AuthService(
                dataService.getUserRepository(),
                dataService.getProfileRepository(),
                validationService
        );
        this.applicantService = new ApplicantService(dataService.getProfileRepository(), validationService);
        this.jobService = new JobService(dataService.getJobRepository(), validationService);
        this.matchingService = new MatchingService();
        this.applicationService = new ApplicationService(
                dataService.getApplicationRepository(),
                dataService.getJobRepository(),
                matchingService,
                new service.AllocationService(dataService.getAllocationRepository())
        );
        this.notificationService = new NotificationService(dataService.getNotificationRepository());
        this.messageService = new MessageService(dataService.getMessageRepository());
        // The dashboard keeps one cached profile object and refreshes it from the
        // repository whenever the workspace regains focus or saves profile changes.
        this.profile = applicantService.getProfileByUserId(currentUser.getUserId());

        setTitle("TA Dashboard - " + Constants.APP_TITLE);
        setSize(1320, 860);
        setMinimumSize(new Dimension(980, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        styleComponents();
        wireActions();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent event) {
                refreshCurrentWorkspaceWithoutEffect();
            }
        });

        JPanel root = new JPanel(new BorderLayout(18, 0));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildWorkspacePanel(), BorderLayout.CENTER);
        add(root);

        // Initial load populates every workspace so tab switches later feel
        // immediate and all unread badges start from a real repository snapshot.
        loadProfile();
        refreshJobs();
        refreshApplications();
        refreshNotifications();
        refreshMessages();
        showWorkspace(VIEW_AVAILABLE_JOBS);
    }

    /**
     * Registers the cross-screen actions used by the TA workspace.
     * Centralizing them here keeps the constructor focused on wiring services
     * and layout, while the event handlers remain easy to scan in one block.
     */
    private void wireActions() {
        // Centralizing the bindings here keeps construction readable and makes
        // it easier to scan all user-triggered flows from one place.
        avatarButton.addActionListener(event -> showProfileMenu());
        viewDetailsButton.addActionListener(event -> viewSelectedJob());
        viewApplicationButton.addActionListener(event -> viewSelectedApplication());
        withdrawButton.addActionListener(event -> withdrawSelectedApplication());
        favouriteButton.addActionListener(event -> toggleSelectedFavouriteJob());
        applyButton.addActionListener(event -> applyForSelectedJob());
        markReadButton.addActionListener(event -> {
            notificationService.markAllRead(currentUser.getUserId());
            refreshNotifications();
            updateUnreadBadges();
        });
        chooseCvButton.addActionListener(event -> chooseCvFile());
        chooseSupportingDocumentButton.addActionListener(event -> chooseSupportingDocumentFile());
        messageMoButton.addActionListener(event -> sendMessageForCurrentSelection());
        replyMessageButton.addActionListener(event -> replyToSelectedMessage());
        markMessagesReadButton.addActionListener(event -> {
            messageService.markAllRead(currentUser.getUserId());
            refreshMessages();
            updateUnreadBadges();
        });
        jobTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    viewSelectedJob();
                }
            }
        });
        favoriteJobTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    viewSelectedJob();
                }
            }
        });
        applicationTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    viewSelectedApplication();
                }
            }
        });
        notificationTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showSelectedNotificationMessage();
                }
            }
        });
        messageTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // Double-click opens the full message body without forcing an
                    // extra toolbar button just for read-only detail viewing.
                    showSelectedConversationMessage();
                }
            }
        });
    }

    /**
     * Builds the left navigation rail with profile identity and workspace tabs.
     */
    private JPanel buildSidebar() {
        // The sidebar stays stable across all TA tasks; only the main card area
        // changes as the applicant moves between opportunities and tracking views.
        JPanel sidebar = new JPanel(new BorderLayout(0, 18));
        sidebar.setPreferredSize(new Dimension(214, 0));
        sidebar.setBackground(new Color(235, 242, 252));
        sidebar.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));

        JPanel accountPanel = new JPanel(new BorderLayout(10, 0));
        accountPanel.setOpaque(false);
        accountPanel.add(avatarButton, BorderLayout.WEST);

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        sidebarNameLabel.setFont(UiTheme.uiFont(Font.BOLD, 15));
        sidebarNameLabel.setForeground(UiTheme.TEXT);
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
        navigation.add(createNavButton("Opportunities", SimpleLineIcon.Type.BRIEFCASE, VIEW_AVAILABLE_JOBS));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavButton("Favourite Jobs", SimpleLineIcon.Type.STAR, VIEW_FAVOURITE_JOBS));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavButton("My Applications", SimpleLineIcon.Type.DOCUMENT, VIEW_APPLICATIONS));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavButton("Notifications", SimpleLineIcon.Type.BELL, VIEW_NOTIFICATIONS));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavButton("Messages", SimpleLineIcon.Type.SEND, VIEW_MESSAGES));
        sidebar.add(navigation, BorderLayout.CENTER);
        return sidebar;
    }

    /**
     * Builds the main card-based content area used by every TA workflow view.
     * The header and contextual action strip stay fixed while the center table
     * swaps between opportunities, favourites, applications, notifications, and messages.
     */
    private JPanel buildWorkspacePanel() {
        // The shell owns the shared title area, filter strip, and contextual
        // actions while CardLayout swaps the actual workspace content below.
        workspaceShell = new JPanel(new BorderLayout(0, 14));
        workspaceShell.setBackground(UiTheme.SURFACE);
        workspaceShell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
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
        contextualActions.setOpaque(false);
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.add(contextualActions);
        header.add(titlePanel, BorderLayout.NORTH);
        header.add(actionRow, BorderLayout.SOUTH);

        // Only job-browsing views need the filter row, so the container stays in
        // the shell and is shown/hidden when the active workspace changes.
        jobFilterContainer.setOpaque(false);
        jobFilterContainer.add(buildJobFilterPanel(), BorderLayout.CENTER);

        workspaceCards.setOpaque(false);
        workspaceCards.add(UiTheme.wrapTable(jobTable), VIEW_KEYS[VIEW_AVAILABLE_JOBS]);
        workspaceCards.add(UiTheme.wrapTable(favoriteJobTable), VIEW_KEYS[VIEW_FAVOURITE_JOBS]);
        workspaceCards.add(UiTheme.wrapTable(applicationTable), VIEW_KEYS[VIEW_APPLICATIONS]);
        workspaceCards.add(buildStatusTablePanel(notificationStatusLabel, notificationTable), VIEW_KEYS[VIEW_NOTIFICATIONS]);
        workspaceCards.add(buildStatusTablePanel(messageStatusLabel, messageTable), VIEW_KEYS[VIEW_MESSAGES]);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(jobFilterContainer, BorderLayout.SOUTH);
        workspaceShell.add(top, BorderLayout.NORTH);
        workspaceShell.add(workspaceCards, BorderLayout.CENTER);
        return workspaceShell;
    }

    /**
     * Creates one sidebar navigation button bound to a workspace card index.
     */
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

    /**
     * Creates a themed action button with a shared line icon style.
     */
    private JButton createIconButton(String text, SimpleLineIcon.Type iconType, boolean primary) {
        // Toolbar-style action buttons all reuse the same icon spacing so the
        // contextual action strip feels consistent across every workspace card.
        JButton button = primary ? UiTheme.createPrimaryButton(text) : UiTheme.createSecondaryButton(text);
        button.setIcon(new SimpleLineIcon(iconType, Color.WHITE));
        button.setIconTextGap(8);
        return button;
    }

    /**
     * Switches the visible workspace card and refreshes the surrounding chrome.
     * Navigation state, header copy, available actions, and unread badges are
     * updated together so the shell never drifts out of sync with the content.
     */
    private void showWorkspace(int viewIndex) {
        currentWorkspaceView = viewIndex;
        CardLayout cardLayout = (CardLayout) workspaceCards.getLayout();
        cardLayout.show(workspaceCards, VIEW_KEYS[viewIndex]);
        for (int i = 0; i < navigationButtons.size(); i++) {
            JToggleButton button = navigationButtons.get(i);
            boolean selected = i == viewIndex;
            button.setSelected(selected);
            button.setBackground(selected ? Color.WHITE : new Color(235, 242, 252));
            button.setForeground(selected ? UiTheme.PRIMARY : UiTheme.TEXT);
            button.setIcon(navIconFor(i, selected));
        }
        updateWorkspaceHeader();
        updateContextualActions();
        updateUnreadBadges();
    }

    /**
     * Returns the icon for one navigation tab, optionally wrapped with an unread badge.
     */
    private Icon navIconFor(int viewIndex, boolean selected) {
        SimpleLineIcon.Type iconType = switch (viewIndex) {
            case VIEW_AVAILABLE_JOBS -> SimpleLineIcon.Type.BRIEFCASE;
            case VIEW_FAVOURITE_JOBS -> SimpleLineIcon.Type.STAR;
            case VIEW_APPLICATIONS -> SimpleLineIcon.Type.DOCUMENT;
            case VIEW_NOTIFICATIONS -> SimpleLineIcon.Type.BELL;
            default -> SimpleLineIcon.Type.SEND;
        };
        boolean unread = (viewIndex == VIEW_NOTIFICATIONS && notificationService.hasUnreadNotifications(currentUser.getUserId()))
                || (viewIndex == VIEW_MESSAGES && messageService.hasUnreadMessages(currentUser.getUserId()));
        return new UnreadBadgeIcon(new SimpleLineIcon(iconType, selected ? UiTheme.PRIMARY : UiTheme.MUTED_TEXT), unread);
    }

    /**
     * Recomputes notification/message unread state and pushes it into the tab
     * icons plus the mark-read action buttons.
     */
    private void updateUnreadBadges() {
        boolean hasUnreadNotifications = notificationService.hasUnreadNotifications(currentUser.getUserId());
        boolean hasUnreadMessages = messageService.hasUnreadMessages(currentUser.getUserId());
        for (int i = 0; i < navigationButtons.size(); i++) {
            JToggleButton button = navigationButtons.get(i);
            button.setIcon(navIconFor(i, button.isSelected()));
        }
        markReadButton.setIcon(new UnreadBadgeIcon(new SimpleLineIcon(SimpleLineIcon.Type.CHECK, Color.WHITE),
                hasUnreadNotifications));
        markMessagesReadButton.setIcon(new UnreadBadgeIcon(new SimpleLineIcon(SimpleLineIcon.Type.CHECK, Color.WHITE),
                hasUnreadMessages));
        markReadButton.setEnabled(hasUnreadNotifications);
        markMessagesReadButton.setEnabled(hasUnreadMessages);
        markReadButton.setVisible(hasUnreadNotifications);
        markMessagesReadButton.setVisible(hasUnreadMessages);
        contextualActions.revalidate();
        contextualActions.repaint();
    }

    /**
     * Rewrites the title, subtitle, and filter visibility for the active card.
     */
    private void updateWorkspaceHeader() {
        switch (currentWorkspaceView) {
            case VIEW_AVAILABLE_JOBS -> {
                workspaceTitleLabel.setText("Opportunities");
                workspaceSubtitleLabel.setText("Search open activities, review requirements, and apply from one focused list.");
                jobFilterContainer.setVisible(true);
            }
            case VIEW_FAVOURITE_JOBS -> {
                workspaceTitleLabel.setText("Favourite Jobs");
                workspaceSubtitleLabel.setText("Keep saved opportunities separate from the full vacancy list.");
                jobFilterContainer.setVisible(true);
            }
            case VIEW_APPLICATIONS -> {
                workspaceTitleLabel.setText("My Applications");
                workspaceSubtitleLabel.setText("Track submitted applications, review decisions, and withdraw eligible records.");
                jobFilterContainer.setVisible(false);
            }
            case VIEW_NOTIFICATIONS -> {
                workspaceTitleLabel.setText("Notifications");
                workspaceSubtitleLabel.setText("Read application, review, and account updates.");
                jobFilterContainer.setVisible(false);
            }
            case VIEW_MESSAGES -> {
                workspaceTitleLabel.setText("Messages");
                workspaceSubtitleLabel.setText("Ask module organisers questions and follow up on replies.");
                jobFilterContainer.setVisible(false);
            }
            default -> {
                workspaceTitleLabel.setText("TA Workspace");
                workspaceSubtitleLabel.setText("");
                jobFilterContainer.setVisible(false);
            }
        }
    }

    /**
     * Rebuilds the right set of actions for the current workspace card.
     * Each view intentionally exposes only the operations that make sense for
     * that context, which keeps the interface focused.
     */
    private void updateContextualActions() {
        contextualActions.removeAll();
        switch (currentWorkspaceView) {
            case VIEW_AVAILABLE_JOBS -> addActions(viewDetailsButton, favouriteButton, messageMoButton, applyButton);
            case VIEW_FAVOURITE_JOBS -> addActions(viewDetailsButton, favouriteButton, messageMoButton, applyButton);
            case VIEW_APPLICATIONS -> addActions(viewApplicationButton, messageMoButton, withdrawButton);
            case VIEW_NOTIFICATIONS -> addActions(markReadButton);
            case VIEW_MESSAGES -> addActions(replyMessageButton, markMessagesReadButton);
            default -> {
            }
        }
        contextualActions.revalidate();
        contextualActions.repaint();
    }

    /**
     * Appends a flat list of action buttons into the contextual action strip.
     */
    private void addActions(JButton... buttons) {
        for (JButton button : buttons) {
            contextualActions.add(button);
        }
    }

    /**
     * Refreshes all TA-facing data and plays the short visual refresh effect.
     */
    private void refreshCurrentWorkspace() {
        playRefreshEffect();
        refreshCurrentWorkspaceWithoutEffect();
    }

    /**
     * Refreshes every table and badge without replaying the background flash.
     * This is used when the frame regains focus so data stays current without
     * feeling visually noisy on every window activation.
     */
    private void refreshCurrentWorkspaceWithoutEffect() {
        // Refresh all cards, not just the visible one, so switching tabs after a
        // profile save or external update always shows up-to-date information.
        profile = applicantService.getProfileByUserId(currentUser.getUserId());
        loadProfile();
        refreshJobs();
        refreshApplications();
        refreshNotifications();
        refreshMessages();
        updateUnreadBadges();
    }

    /**
     * Plays a brief background flash after manual refresh-style actions.
     */
    private void playRefreshEffect() {
        if (workspaceShell == null) {
            return;
        }
        // A short shell-background flash acknowledges manual refreshes without
        // adding persistent animation that could distract from dense tables.
        Color original = workspaceShell.getBackground();
        Color[] frames = {
                new Color(225, 236, 255),
                new Color(239, 246, 255),
                UiTheme.SURFACE
        };
        final int[] index = {0};
        Timer timer = new Timer(80, null);
        timer.addActionListener(event -> {
            workspaceShell.setBackground(frames[index[0]]);
            workspaceShell.repaint();
            index[0]++;
            if (index[0] >= frames.length) {
                workspaceShell.setBackground(original);
                workspaceShell.repaint();
                timer.stop();
            }
        });
        timer.start();
    }

    /**
     * Opens the profile/account menu anchored to the avatar button.
     */
    private void showProfileMenu() {
        // Profile/account actions are grouped in a popup so the main toolbar can
        // stay focused on job and application workflow actions instead.
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        menu.add(buildProfileMenuHeader());
        menu.addSeparator();
        menu.add(menuItem("Edit Profile", SimpleLineIcon.Type.EDIT, this::showProfileDialog));
        menu.add(menuItem("Change Password", SimpleLineIcon.Type.SAVE, this::showChangePasswordDialog));
        menu.add(menuItem("Open CV", SimpleLineIcon.Type.FILE, this::openCvFile));
        menu.add(menuItem("Open Supporting Documents", SimpleLineIcon.Type.DOCUMENT, this::openSupportingDocumentFile));
        menu.addSeparator();
        menu.add(menuItem("Refresh", SimpleLineIcon.Type.REFRESH, this::refreshCurrentWorkspace));
        menu.add(menuItem("Logout", SimpleLineIcon.Type.LOGOUT, this::returnToLogin));
        menu.addSeparator();
        JMenuItem deleteItem = menuItem("Delete Account", SimpleLineIcon.Type.TRASH, this::deleteCurrentAccount);
        deleteItem.setForeground(UiTheme.DANGER);
        menu.add(deleteItem);
        menu.show(avatarButton, 0, avatarButton.getHeight() + 6);
    }

    /**
     * Builds the identity header shown at the top of the profile popup menu.
     */
    private JPanel buildProfileMenuHeader() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(UiTheme.SURFACE);
        AvatarButton preview = new AvatarButton(initialsForProfile());
        preview.setEnabled(false);
        panel.add(preview, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(valueOrDash(profile.getName()));
        name.setFont(UiTheme.uiFont(Font.BOLD, 16));
        name.setForeground(UiTheme.TEXT);
        JLabel email = new JLabel(valueOrDash(profile.getEmail()));
        email.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        email.setForeground(UiTheme.MUTED_TEXT);
        JLabel programme = new JLabel("Programme: " + valueOrDash(profile.getProgramme()));
        programme.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        programme.setForeground(UiTheme.MUTED_TEXT);
        text.add(name);
        text.add(Box.createVerticalStrut(4));
        text.add(email);
        text.add(Box.createVerticalStrut(3));
        text.add(programme);
        panel.add(text, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(320, 74));
        return panel;
    }

    /**
     * Creates one popup-menu item with shared icon and spacing rules.
     */
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

    /**
     * Opens the standalone password-change dialog for the current TA.
     */
    private void showChangePasswordDialog() {
        new ChangePasswordFrame(authService, currentUser).setVisible(true);
    }

    /**
     * Opens the modal profile editor that owns all contact, academic, skill,
     * and document upload fields for the applicant.
     */
    private void showProfileDialog() {
        loadProfile();
        // The modal editor owns all profile fields so the main workspace tables
        // never need to make room for a large inline form.
        JDialog dialog = new JDialog(this, "Edit Profile", true);
        dialog.setSize(600, 720);
        dialog.setMinimumSize(new Dimension(520, 560));
        dialog.setLocationRelativeTo(this);

        JPanel content = UiTheme.createCard("Applicant Profile", "Update your contact, academic, skills, and document information.");
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
        UiTheme.addFormRow(form, 20, "Supporting Documents", buildSupportingDocumentPicker());

        JButton closeButton = createIconButton("Close", SimpleLineIcon.Type.LOGOUT, false);
        JButton saveButton = createIconButton("Save Profile", SimpleLineIcon.Type.SAVE, true);
        closeButton.addActionListener(event -> dialog.dispose());
        saveButton.addActionListener(event -> saveProfile());

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(UiTheme.wrapPage(form), BorderLayout.CENTER);
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, closeButton, saveButton), BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
        dialog.setContentPane(UiTheme.wrapPage(content));
        dialog.setVisible(true);
    }

    /**
     * Builds the opportunity-search controls and wires them to live refreshes.
     */
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
                // Plain text fields rarely fire this in Swing, but implementing
                // all three callbacks keeps the listener complete and reusable.
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

    /**
     * Wraps a table with its small status summary line used by notifications and messages.
     */
    private JPanel buildStatusTablePanel(JLabel statusLabel, JTable table) {
        statusLabel.setForeground(UiTheme.MUTED_TEXT);
        statusLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(UiTheme.wrapTable(table), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds a compact label-plus-control block for the job filter row.
     */
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
        // The visible form fields are always reloaded from the cached profile
        // object so dialogs and sidebar identity stay in sync after updates.
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
        supportingDocumentPathField.setText(joinDocumentPaths(profile.getSupportingDocumentPaths()));
        updateSupportingDocumentOpenState();
        avatarButton.setInitials(initialsForProfile());
        sidebarNameLabel.setText(valueOrDash(profile.getName()));
        sidebarRoleLabel.setText("TA Applicant");
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
            List<String> supportingDocumentPaths = parseDocumentPaths(supportingDocumentPathField.getText());
            for (String supportingDocumentPath : supportingDocumentPaths) {
                profileErrors.addAll(validationService.validateSupportingDocumentPath(supportingDocumentPath));
            }
            if (!profileErrors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\n", profileErrors));
            }
            // CV and supporting documents are stored through CvStorageService so
            // the profile ultimately saves managed relative paths rather than raw chooser paths.
            String storedCvPath = cvStorageService.storeCvForApplicant(
                    profile.getApplicantId(),
                    cvPathField.getText().trim(),
                    profile.getCvPath()
            );
            List<String> storedSupportingDocumentPaths = cvStorageService.storeSupportingDocumentsForApplicant(
                    profile.getApplicantId(),
                    supportingDocumentPaths,
                    profile.getSupportingDocumentPaths()
            );
            profile.setCvPath(storedCvPath);
            profile.setSupportingDocumentPaths(storedSupportingDocumentPaths);
            applicantService.saveProfile(profile);
            cvPathField.setText(profile.getCvPath());
            supportingDocumentPathField.setText(joinDocumentPaths(profile.getSupportingDocumentPaths()));
            updateCvPathOpenState();
            updateSupportingDocumentOpenState();
            avatarButton.setInitials(initialsForProfile());
            sidebarNameLabel.setText(valueOrDash(profile.getName()));
            sidebarRoleLabel.setText("TA Applicant");
            UiMessage.info(this, "Profile saved successfully.");
            // Refresh dependent tables because a profile edit can change match
            // scores, favourite indicators, and application completeness checks.
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

        // Filters are resolved up front so both the full opportunities list and
        // the favourites subset are built from the exact same filtered job set.
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
            // Favourites are presented as a filtered projection of the same job
            // rows rather than as a separately queried dataset.
            if (profile != null && profile.isFavoriteJob(job.getJobId())) {
                favoriteJobTableModel.addRow(row);
            }
        }
    }

    /**
     * Rebuilds filter dropdown contents from currently open jobs while trying to
     * preserve the user's existing selections where possible.
     */
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

    /**
     * Checks whether a combo box still contains a previously selected value.
     */
    private boolean comboContains(JComboBox<String> comboBox, String value) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (value.equals(comboBox.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the category filter display text back to its enum value.
     */
    private JobCategory selectedCategoryFilter() {
        String selected = String.valueOf(categoryFilterBox.getSelectedItem());
        for (JobCategory category : JobCategory.values()) {
            if (category.getDisplayName().equals(selected)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Builds one table row for both the full opportunities list and favourites list.
     */
    private Object[] buildJobRow(JobPosting job) {
        return new Object[]{
                job.getJobId(),
                job.getModuleCode() + " - " + job.getModuleTitle(),
                moduleOrganiserText(job),
                UiFormat.valueOrDash(job.getJobType()),
                valueOrDash(job.getSchedule()),
                valueOrDash(job.getLocation()),
                UiFormat.workload(job),
                buildTaDemandText(job),
                job.getApplicationDeadline(),
                String.join(", ", job.getRequiredSkills()),
                matchScoreText(job),
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
            // Reviewer notes are normalised to a placeholder so rejected or
            // pending applications do not display visually empty cells.
            applicationTableModel.addRow(new Object[]{
                    record.getApplicationId(),
                    record.getJobId(),
                    record.getStatus(),
                    record.getMatchScore(),
                    String.join(", ", record.getMissingSkills()),
                    reviewerNotesOrPending(record.getReviewerNotes())
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
        showJobDetailsDialog(job, taDemandSummary);
    }

    /**
     * Opens a TA-facing job dialog with the same details layout plus the core
     * applicant actions embedded below the summary.
     */
    private void showJobDetailsDialog(JobPosting job, String taDemandSummary) {
        new JobDetailsDialog(
                this,
                job,
                taDemandSummary,
                moduleOrganiserText(job),
                buildJobDialogActions(job)
        ).setVisible(true);
    }

    /**
     * Builds the action row shown at the bottom of TA job details.
     */
    private JPanel buildJobDialogActions(JobPosting job) {
        JButton favouriteAction = createIconButton("Toggle Favourite", SimpleLineIcon.Type.STAR, false);
        JButton messageAction = createIconButton("Message MO", SimpleLineIcon.Type.SEND, false);
        JButton applyAction = createIconButton("Apply", SimpleLineIcon.Type.SEND, true);
        JButton closeAction = createIconButton("Close", SimpleLineIcon.Type.LOGOUT, false);

        favouriteAction.addActionListener(event -> toggleFavouriteJob(job.getJobId()));
        messageAction.addActionListener(event -> sendMessageForJob(job));
        applyAction.addActionListener(event -> applyForJob(job));
        closeAction.addActionListener(event -> SwingUtilities.getWindowAncestor(closeAction).dispose());

        return UiTheme.createButtonRow(FlowLayout.RIGHT, favouriteAction, messageAction, applyAction, closeAction);
    }

    /**
     * Opens the structured details dialog for the selected application.
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

        showApplicationDetailsDialog(application);
    }

    /**
     * Opens a TA-facing application dialog with the core follow-up actions below it.
     */
    private void showApplicationDetailsDialog(ApplicationRecord application) {
        JobPosting job = findJob(application.getJobId()).orElse(null);
        new ApplicationDetailsDialog(
                this,
                application,
                job,
                job == null ? null : buildTaDemandText(job),
                job == null ? null : moduleOrganiserText(job),
                buildApplicationDialogActions(application, job)
        ).setVisible(true);
    }

    /**
     * Builds the action row shown at the bottom of TA application details.
     */
    private JPanel buildApplicationDialogActions(ApplicationRecord application, JobPosting job) {
        JButton messageAction = createIconButton("Message MO", SimpleLineIcon.Type.SEND, false);
        JButton withdrawAction = UiTheme.createDangerButton("Withdraw Application");
        JButton closeAction = createIconButton("Close", SimpleLineIcon.Type.LOGOUT, false);
        withdrawAction.setIcon(new SimpleLineIcon(SimpleLineIcon.Type.LOGOUT, Color.WHITE));
        withdrawAction.setIconTextGap(8);

        messageAction.addActionListener(event -> {
            JobPosting latestJob = findJob(application.getJobId()).orElse(null);
            if (latestJob == null) {
                UiMessage.error(this, "This job is no longer available. Please refresh the workspace.");
                return;
            }
            sendMessageForJob(latestJob, application);
        });
        withdrawAction.addActionListener(event -> withdrawApplication(application.getApplicationId()));
        closeAction.addActionListener(event -> SwingUtilities.getWindowAncestor(closeAction).dispose());

        messageAction.setEnabled(job != null);
        return UiTheme.createButtonRow(FlowLayout.RIGHT, messageAction, withdrawAction, closeAction);
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
        JobPosting job = findJob(jobId).orElse(null);
        if (job == null) {
            UiMessage.error(this, "This job is no longer available. Please refresh the table.");
            refreshJobs();
            return;
        }
        applyForJob(job);
    }

    /**
     * Submits an application for a specific job.
     */
    private void applyForJob(JobPosting job) {
        List<String> profileIssues = getProfileIssuesForApplication();
        if (!profileIssues.isEmpty()) {
            // The TA must fix profile gaps before applying so downstream matching,
            // recruiter review, and file access all have the required data.
            UiMessage.error(this,
                    "Please complete your profile before applying.\nMissing or invalid items:\n- "
                            + String.join("\n- ", profileIssues)
                            + "\n\nSave your profile first, then try again.");
            return;
        }

        try {
            JobPosting latestJob = findJob(job.getJobId())
                    .orElseThrow(() -> new IllegalStateException("This job is no longer available. Please refresh the table."));
            // ApplicationService owns duplicate checks, matching calculation, and
            // persistence; the dashboard just coordinates the user feedback.
            ApplicationRecord record = applicationService.apply(profile, latestJob);
            notificationService.notifyUser(currentUser.getUserId(), "Application submitted for " + latestJob.getModuleCode() + " " + latestJob.getModuleTitle() + ".");
            notificationService.notifyUser(latestJob.getPostedBy(), profile.getName() + " submitted an application for " + latestJob.getModuleCode() + " " + latestJob.getModuleTitle() + ".");
            UiMessage.info(this,
                    "Application submitted for " + latestJob.getModuleCode() + " - " + latestJob.getModuleTitle()
                            + ".\nMatch score: " + record.getMatchScore()
                            + "%\nYou can track updates in My Applications.");
            refreshApplications();
            refreshJobs();
            refreshNotifications();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Reloads the notification inbox table and updates its unread summary line.
     */
    private void refreshNotifications() {
        notificationTableModel.setRowCount(0);
        int unreadCount = 0;
        for (NotificationRecord notification : notificationService.getNotificationsForUser(currentUser.getUserId())) {
            if (!notification.isRead()) {
                unreadCount++;
            }
            notificationTableModel.addRow(new Object[]{
                    notification.getNotificationId(),
                    UiFormat.dateTime(notification.getCreatedAt()),
                    notification.isRead() ? "Read" : "New",
                    notification.getMessage()
            });
        }
        notificationStatusLabel.setText(unreadCount == 0
                ? "All notifications are read."
                : "Unread notifications: " + unreadCount + ". Click Mark Notifications Read to clear the badge.");
        // Badge state is recalculated here so the sidebar and top actions stay
        // aligned with the table content the user is currently seeing.
        updateUnreadBadges();
    }

    /**
     * Reloads the conversation table and recalculates unread incoming messages.
     */
    private void refreshMessages() {
        messageTableModel.setRowCount(0);
        int unreadCount = 0;
        for (MessageRecord message : messageService.getConversationForUser(currentUser.getUserId())) {
            // Each row flattens job context, direction, and status into one
            // grid so the TA can triage conversations without opening each thread.
            JobPosting job = findJob(message.getJobId()).orElse(null);
            boolean incoming = currentUser.getUserId().equals(message.getRecipientUserId());
            if (incoming && !message.isRead()) {
                unreadCount++;
            }
            messageTableModel.addRow(new Object[]{
                    message.getMessageId(),
                    UiFormat.dateTime(message.getCreatedAt()),
                    job == null ? valueOrDash(message.getJobId()) : job.getModuleCode() + " - " + job.getModuleTitle(),
                    incoming ? "Incoming from " + displayNameForUser(message.getSenderUserId()) : "Outgoing to " + displayNameForUser(message.getRecipientUserId()),
                    incoming && !message.isRead() ? "New" : "Read",
                    message.getBody()
            });
        }
        messageStatusLabel.setText(unreadCount == 0
                ? "All incoming messages are read."
                : "Unread incoming messages: " + unreadCount + ". Click Mark Messages Read to clear the badge.");
        updateUnreadBadges();
    }

    /**
     * Opens a simple detail dialog for the currently selected notification row.
     */
    private void showSelectedNotificationMessage() {
        NotificationRecord selected = selectedNotificationRecord();
        if (selected == null) {
            return;
        }
        new NotificationDetailsDialog(this, selected).setVisible(true);
    }

    /**
     * Opens the structured detail dialog for the currently selected message row.
     */
    private void showSelectedConversationMessage() {
        MessageRecord selected = selectedMessageRecord();
        if (selected == null) {
            return;
        }
        JobPosting job = findJob(selected.getJobId()).orElse(null);
        boolean incoming = currentUser.getUserId().equals(selected.getRecipientUserId());
        String direction = incoming
                ? "Incoming from " + displayNameForUser(selected.getSenderUserId())
                : "Outgoing to " + displayNameForUser(selected.getRecipientUserId());
        String status = incoming && !selected.isRead() ? "New" : "Read";
        new MessageDetailsDialog(this, selected, job, direction, status, buildMessageDialogActions(selected)).setVisible(true);
    }

    /**
     * Builds the action row shown at the bottom of TA message details.
     */
    private JPanel buildMessageDialogActions(MessageRecord selected) {
        JButton replyAction = createIconButton("Reply", SimpleLineIcon.Type.SEND, true);
        JButton closeAction = createIconButton("Close", SimpleLineIcon.Type.LOGOUT, false);

        replyAction.addActionListener(event -> replyToMessage(selected));
        closeAction.addActionListener(event -> SwingUtilities.getWindowAncestor(closeAction).dispose());

        return UiTheme.createButtonRow(FlowLayout.RIGHT, replyAction, closeAction);
    }

    /**
     * Sends a new message to the MO about either the selected job or the job
     * linked to the selected application, depending on the active workspace.
     */
    private void sendMessageForCurrentSelection() {
        ApplicationRecord application = currentWorkspaceView == VIEW_APPLICATIONS ? selectedApplicationRecord() : null;
        String jobId = application == null ? selectedJobId() : application.getJobId();
        if (jobId == null) {
            UiMessage.error(this, "Please select a job or application before sending a message.");
            return;
        }

        JobPosting job = findJob(jobId).orElse(null);
        if (job == null) {
            UiMessage.error(this, "This job is no longer available. Please refresh the workspace.");
            return;
        }
        sendMessageForJob(job, application);
    }

    /**
     * Sends a new TA-to-MO message for a specific job.
     */
    private void sendMessageForJob(JobPosting job) {
        sendMessageForJob(job, null);
    }

    /**
     * Sends a new TA-to-MO message for a job and optional application context.
     */
    private void sendMessageForJob(JobPosting job, ApplicationRecord application) {
        String message = promptForMessage(
                "Message MO",
                "Send a message to the organiser of " + job.getModuleCode() + " - " + job.getModuleTitle() + "."
        ).orElse(null);
        if (message == null) {
            return;
        }

        try {
            // Message threads keep both job and optional application context so
            // follow-up replies still make sense from either dashboard side.
            MessageRecord sent = messageService.sendMessage(
                    job.getJobId(),
                    application == null ? null : application.getApplicationId(),
                    currentUser.getUserId(),
                    job.getPostedBy(),
                    message
            );
            notificationService.notifyUser(job.getPostedBy(), profile.getName() + " sent a message about " + job.getModuleCode() + " " + job.getModuleTitle() + ".");
            UiMessage.info(this, "Message sent. Reference: " + sent.getMessageId() + ".");
            refreshMessages();
            refreshNotifications();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Replies to the selected message while preserving its original job/application context.
     */
    private void replyToSelectedMessage() {
        MessageRecord selected = selectedMessageRecord();
        if (selected == null) {
            UiMessage.error(this, "Please select a message before replying.");
            return;
        }
        replyToMessage(selected);
    }

    /**
     * Replies to a specific message while preserving its original job/application context.
     */
    private void replyToMessage(MessageRecord selected) {
        String recipientUserId = currentUser.getUserId().equals(selected.getSenderUserId())
                ? selected.getRecipientUserId()
                : selected.getSenderUserId();
        String message = promptForMessage("Reply", "Reply to " + displayNameForUser(recipientUserId) + ".").orElse(null);
        if (message == null) {
            return;
        }

        try {
            // Replies preserve the original job/application linkage instead of
            // starting a detached conversation record with no recruiting context.
            MessageRecord reply = messageService.sendMessage(
                    selected.getJobId(),
                    selected.getApplicationId(),
                    currentUser.getUserId(),
                    recipientUserId,
                    message
            );
            messageService.markRead(selected.getMessageId(), currentUser.getUserId());
            notificationService.notifyUser(recipientUserId, profile.getName() + " replied to your message.");
            UiMessage.info(this, "Reply sent. Reference: " + reply.getMessageId() + ".");
            refreshMessages();
        } catch (Exception ex) {
            UiMessage.error(this, ex.getMessage());
        }
    }

    /**
     * Prompts for a free-text message body and rejects empty submissions.
     */
    private Optional<String> promptForMessage(String title, String helperText) {
        JTextArea messageArea = new JTextArea(6, 42);
        UiTheme.styleTextArea(messageArea, 6);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        JTextArea helperArea = new JTextArea(helperText);
        helperArea.setEditable(false);
        helperArea.setOpaque(false);
        helperArea.setLineWrap(true);
        helperArea.setWrapStyleWord(true);
        helperArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(helperArea, BorderLayout.NORTH);
        panel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        String message = messageArea.getText().trim();
        if (message.isBlank()) {
            UiMessage.error(this, "Message cannot be empty.");
            return Optional.empty();
        }
        return Optional.of(message);
    }

    /**
     * Resolves the selected application table row back to the corresponding domain record.
     */
    private ApplicationRecord selectedApplicationRecord() {
        int row = applicationTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String applicationId = String.valueOf(applicationTableModel.getValueAt(row, 0));
        return applicationService.getApplicationsForApplicant(profile.getApplicantId()).stream()
                .filter(record -> applicationId.equals(record.getApplicationId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the selected message table row back to the corresponding domain record.
     */
    private MessageRecord selectedMessageRecord() {
        int row = messageTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String messageId = String.valueOf(messageTableModel.getValueAt(row, 0));
        return messageService.getConversationForUser(currentUser.getUserId()).stream()
                .filter(message -> messageId.equals(message.getMessageId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the selected notification table row back to the repository record.
     */
    private NotificationRecord selectedNotificationRecord() {
        int row = notificationTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String notificationId = String.valueOf(notificationTableModel.getValueAt(row, 0));
        return notificationService.getNotificationsForUser(currentUser.getUserId()).stream()
                .filter(notification -> notificationId.equals(notification.getNotificationId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Looks up a user-friendly display name for a user id used in message rows.
     */
    private String displayNameForUser(String userId) {
        return dataService.getUserRepository().findAll().stream()
                .filter(user -> userId != null && userId.equals(user.getUserId()))
                .findFirst()
                .map(user -> valueOrDash(user.getName()))
                .orElse(valueOrDash(userId));
    }

    /**
     * Resolves the MO attached to a job for TA-facing job tables and details.
     */
    private String moduleOrganiserText(JobPosting job) {
        if (job == null || job.getPostedBy() == null || job.getPostedBy().isBlank()) {
            return "-";
        }
        return dataService.getUserRepository().findAll().stream()
                .filter(user -> job.getPostedBy().equals(user.getUserId()))
                .findFirst()
                .map(user -> {
                    String name = valueOrDash(user.getName());
                    String email = valueOrDash(user.getUsername());
                    if ("-".equals(name)) {
                        return email;
                    }
                    if ("-".equals(email)) {
                        return name;
                    }
                    return name + " <" + email + ">";
                })
                .orElse(valueOrDash(job.getPostedBy()));
    }

    private String matchScoreText(JobPosting job) {
        if (profile == null || job == null) {
            return "-";
        }
        return matchingService.calculateMatch(profile.getSkills(), job.getRequiredSkills()).getScorePercentage() + "%";
    }

    /**
     * Toggles the selected job in the applicant's favourite-job list.
     */
    private void toggleSelectedFavouriteJob() {
        String jobId = selectedJobId();
        if (jobId == null) {
            UiMessage.error(this, "Please select a job before changing favourites.");
            return;
        }
        toggleFavouriteJob(jobId);
    }

    /**
     * Toggles a specific job in the applicant's favourite-job list.
     */
    private void toggleFavouriteJob(String jobId) {
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
        withdrawApplication(applicationId);
    }

    /**
     * Withdraws a specific application after confirmation.
     */
    private void withdrawApplication(String applicationId) {
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

    /**
     * Returns the selected job id from the active job table, if the current view uses one.
     */
    private String selectedJobId() {
        if (currentWorkspaceView != VIEW_AVAILABLE_JOBS && currentWorkspaceView != VIEW_FAVOURITE_JOBS) {
            return null;
        }
        JTable selectedTable = currentWorkspaceView == VIEW_FAVOURITE_JOBS ? favoriteJobTable : jobTable;
        DefaultTableModel model = selectedTable == favoriteJobTable ? favoriteJobTableModel : jobTableModel;
        int row = selectedTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return String.valueOf(model.getValueAt(row, 0));
    }

    /**
     * Persists the cached profile object directly when only lightweight state like
     * favourites changes and full profile validation would add noise.
     */
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
            Set<String> deletedApplicationIds = dataService.getApplicationRepository().findByApplicantId(profile.getApplicantId()).stream()
                    .map(ApplicationRecord::getApplicationId)
                    .collect(Collectors.toSet());
            applicationService.removeApplicationsForApplicant(profile.getApplicantId());
            cvStorageService.deleteManagedCv(profile.getCvPath());
            cvStorageService.deleteManagedSupportingDocuments(profile.getSupportingDocumentPaths());
            new AccountCleanupService(
                    dataService.getAllocationRepository(),
                    dataService.getMessageRepository(),
                    dataService.getNotificationRepository()
            ).cleanupDeletedApplicant(profile.getApplicantId(), currentUser.getUserId(), deletedApplicationIds);

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
     * Builds the read-only supporting-document field plus choose-files button.
     */
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

    /**
     * Opens the file chooser for one or more supporting documents.
     */
    private void chooseSupportingDocumentFile() {
        chooseDocumentFile(supportingDocumentPathField, false);
    }

    /**
     * Opens the shared document chooser for CVs or supporting documents and
     * writes the selected path(s) back into the profile form.
     */
    private void chooseDocumentFile(JTextField targetField, boolean cvFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(cvFile ? "Select CV File" : "Select Supporting Documents");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(!cvFile);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
                cvFile ? CV_FILE_DESCRIPTION : "Supporting Documents (*.pdf, *.doc, *.docx, *.rtf, *.txt)",
                "pdf", "doc", "docx", "rtf", "txt"
        ));
        UiTheme.styleFileChooser(chooser);

        String existingPath = cvFile ? targetField.getText().trim() : firstDocumentPath(targetField.getText());
        if (!existingPath.isBlank()) {
            File existingFile = cvStorageService.resolveCvPath(existingPath).toFile();
            if (existingFile.exists()) {
                chooser.setSelectedFile(existingFile);
            }
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            List<String> selectedPaths = selectedDocumentPaths(chooser, cvFile);
            List<String> errors = new ArrayList<>();
            for (String selectedPath : selectedPaths) {
                errors.addAll(cvFile
                        ? validationService.validateCvPath(selectedPath)
                        : validationService.validateSupportingDocumentPath(selectedPath));
            }
            if (!errors.isEmpty()) {
                UiMessage.error(this, String.join("\n", errors));
                return;
            }
            targetField.setText(cvFile ? selectedPaths.get(0) : joinDocumentPaths(selectedPaths));
            if (cvFile) {
                updateCvPathOpenState();
            } else {
                updateSupportingDocumentOpenState();
            }
        }
    }

    /**
     * Updates cursor and tooltip affordances for the CV path field.
     */
    private void updateCvPathOpenState() {
        String cvPath = cvPathField.getText().trim();
        boolean hasCvPath = !cvPath.isBlank();
        cvPathField.setCursor(Cursor.getPredefinedCursor(hasCvPath ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        cvPathField.setToolTipText(hasCvPath ? "Click to open " + cvPath : "Choose your CV file to fill this path automatically.");
    }

    /**
     * Updates cursor and tooltip affordances for the supporting-document field.
     */
    private void updateSupportingDocumentOpenState() {
        List<String> documentPaths = parseDocumentPaths(supportingDocumentPathField.getText());
        boolean hasDocument = !documentPaths.isEmpty();
        supportingDocumentPathField.setCursor(Cursor.getPredefinedCursor(hasDocument ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        supportingDocumentPathField.setToolTipText(hasDocument
                ? "Click to open one of " + documentPaths.size() + " supporting document(s)."
                : "Optionally choose transcripts, certificates, or supporting documents.");
    }

    /**
     * Opens the currently stored CV path through the shared document opener.
     */
    private void openCvFile() {
        openDocumentFile(cvPathField.getText().trim(), "CV file");
    }

    /**
     * Opens one of the selected supporting documents, prompting first if needed.
     */
    private void openSupportingDocumentFile() {
        List<String> documentPaths = parseDocumentPaths(supportingDocumentPathField.getText());
        if (documentPaths.isEmpty()) {
            UiMessage.error(this, "No supporting document is available. Please choose a file first.");
            return;
        }
        if (documentPaths.size() == 1) {
            openDocumentFile(documentPaths.get(0), "supporting document");
            return;
        }
        String selectedPath = (String) JOptionPane.showInputDialog(
                this,
                "Choose a supporting document to open:",
                "Open Supporting Document",
                JOptionPane.PLAIN_MESSAGE,
                null,
                documentPaths.toArray(new String[0]),
                documentPaths.get(0)
        );
        if (selectedPath != null && !selectedPath.isBlank()) {
            openDocumentFile(selectedPath, "supporting document");
        }
    }

    /**
     * Extracts the chosen path or paths from the document chooser result.
     */
    private List<String> selectedDocumentPaths(JFileChooser chooser, boolean cvFile) {
        List<String> paths = new ArrayList<>();
        if (cvFile) {
            paths.add(chooser.getSelectedFile().getAbsolutePath());
            return paths;
        }
        for (File selectedFile : chooser.getSelectedFiles()) {
            paths.add(selectedFile.getAbsolutePath());
        }
        if (paths.isEmpty() && chooser.getSelectedFile() != null) {
            paths.add(chooser.getSelectedFile().getAbsolutePath());
        }
        return paths;
    }

    /**
     * Parses a semicolon- or newline-separated document list from the form field.
     */
    private List<String> parseDocumentPaths(String documentText) {
        List<String> paths = new ArrayList<>();
        if (documentText == null || documentText.isBlank()) {
            return paths;
        }
        for (String path : documentText.split("[;\\n]")) {
            if (!path.trim().isBlank()) {
                paths.add(path.trim());
            }
        }
        return paths;
    }

    /**
     * Returns the first parsed document path, used to seed the file chooser.
     */
    private String firstDocumentPath(String documentText) {
        List<String> paths = parseDocumentPaths(documentText);
        return paths.isEmpty() ? "" : paths.get(0);
    }

    /**
     * Joins document paths back into the compact single-field display format.
     */
    private String joinDocumentPaths(List<String> documentPaths) {
        if (documentPaths == null || documentPaths.isEmpty()) {
            return "";
        }
        return String.join("; ", documentPaths);
    }

    /**
     * Resolves a stored document path and asks the desktop OS to open it.
     */
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
        chooseCvButton.setIcon(new SimpleLineIcon(SimpleLineIcon.Type.FILE, Color.WHITE));
        chooseCvButton.setIconTextGap(8);
        chooseSupportingDocumentButton.setIcon(new SimpleLineIcon(SimpleLineIcon.Type.DOCUMENT, Color.WHITE));
        chooseSupportingDocumentButton.setIconTextGap(8);
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
        supportingDocumentPathField.setToolTipText("Optionally choose transcripts, certificates, or supporting documents.");
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
        UiTheme.styleTable(messageTable);
        jobTable.getColumnModel().getColumn(7).setCellRenderer(new TaDemandRenderer());
        jobTable.getColumnModel().getColumn(11).setCellRenderer(new FavouriteRenderer());
        // The deadline and status renderers must target the actual deadline/status
        // columns rather than the TA-demand/favourite columns.
        jobTable.getColumnModel().getColumn(8).setCellRenderer(new DeadlineWarningRenderer());
        jobTable.getColumnModel().getColumn(12).setCellRenderer(new StatusBadgeRenderer());
        favoriteJobTable.getColumnModel().getColumn(7).setCellRenderer(new TaDemandRenderer());
        favoriteJobTable.getColumnModel().getColumn(11).setCellRenderer(new FavouriteRenderer());
        favoriteJobTable.getColumnModel().getColumn(8).setCellRenderer(new DeadlineWarningRenderer());
        favoriteJobTable.getColumnModel().getColumn(12).setCellRenderer(new StatusBadgeRenderer());
        applicationTable.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer());
        UiTheme.setColumnWidths(jobTable, 90, 260, 240, 140, 260, 180, 110, 100, 120, 220, 100, 90, 90);
        UiTheme.setColumnWidths(favoriteJobTable, 90, 260, 240, 140, 260, 180, 110, 100, 120, 220, 100, 90, 90);
        UiTheme.setColumnWidths(applicationTable, 120, 90, 120, 90, 220, 280);
        UiTheme.setColumnWidths(notificationTable, 130, 160, 80, 560);
        hideNotificationIdColumn();
        UiTheme.setColumnWidths(messageTable, 110, 160, 230, 190, 80, 520);
    }

    /**
     * Keeps the notification id in the model for reliable lookup while leaving
     * the visible table focused on time, status, and message.
     */
    private void hideNotificationIdColumn() {
        if (notificationTable.getColumnModel().getColumnCount() == notificationTableModel.getColumnCount()) {
            TableColumn idColumn = notificationTable.getColumnModel().getColumn(0);
            notificationTable.getColumnModel().removeColumn(idColumn);
        }
    }

    /**
     * Returns a dash for null or blank text to keep UI output readable.
     */
    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * Provides a stable placeholder for applications that have not been reviewed yet.
     */
    private String reviewerNotesOrPending(String value) {
        return value == null || value.isBlank() ? "Not yet reviewed" : value;
    }

    /**
     * Derives avatar initials from the current profile or fallback login name.
     */
    private String initialsForProfile() {
        String displayName = profile == null ? null : profile.getName();
        if (displayName == null || displayName.isBlank()) {
            displayName = currentUser.getUsername();
        }
        if (displayName == null || displayName.isBlank()) {
            return "TA";
        }
        String trimmed = displayName.trim();
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
            return builder.isEmpty() ? "TA" : builder.toString();
        }
        return trimmed.length() <= 2 ? trimmed : trimmed.substring(0, 2);
    }

    /**
     * Converts missing skills into a short applicant-facing suggestion sentence.
     */
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

    /**
     * Collects the profile gaps that would block a valid application submission.
     * The method reports both missing required fields and richer validation errors
     * so the TA can fix everything in one pass.
     */
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
     * Renderer that highlights whether a job still has remaining TA capacity.
     */
    private static class TaDemandRenderer extends DefaultTableCellRenderer {
        private static final Color LIGHT_GREEN = new Color(205, 235, 214);
        private static final Color LIGHT_RED = new Color(242, 205, 205);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (isSelected) {
                return label;
            }

            label.setForeground(Color.BLACK);
            label.setBackground(Color.WHITE);
            String text = value == null ? "" : value.toString().trim();
            String[] parts = text.split("/");
            if (parts.length == 2) {
                try {
                    int accepted = Integer.parseInt(parts[0].trim());
                    int required = Integer.parseInt(parts[1].trim());
                    label.setBackground(accepted < required ? LIGHT_GREEN : LIGHT_RED);
                } catch (NumberFormatException ignored) {
                    label.setBackground(Color.WHITE);
                }
            }
            return label;
        }
    }

    /**
     * Renderer that softly highlights favourite state without obscuring text.
     */
    private static class FavouriteRenderer extends DefaultTableCellRenderer {
        private static final Color LIGHT_GREEN = new Color(205, 235, 214);
        private static final Color LIGHT_RED = new Color(242, 205, 205);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (isSelected) {
                return label;
            }

            String text = value == null ? "" : value.toString().trim();
            label.setForeground(Color.BLACK);
            if ("Yes".equalsIgnoreCase(text)) {
                label.setBackground(LIGHT_GREEN);
            } else if ("No".equalsIgnoreCase(text)) {
                label.setBackground(LIGHT_RED);
            } else {
                label.setBackground(Color.WHITE);
            }
            return label;
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
                case "INTERVIEW_INVITED" -> label.setBackground(new Color(229, 241, 255));
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
