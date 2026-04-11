package ui;

import model.Role;
import model.User;
import service.AuthService;
import service.DataService;
import service.ValidationService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * The main login window for the TA Recruitment System.
 * This frame serves as the entry point for user authentication and provides options
 * for loading demo data and registering new TA accounts.
 *
 * Key features:
 * - User authentication with username, password, and role selection
 * - Password visibility toggle for better usability
 * - Demo account information display
 * - Sample data loading functionality
 * - Registration link for new TA accounts
 * - Role-based dashboard redirection after successful login
 *
 * The UI is designed with a split-pane layout featuring a hero section
 * with system information and a login form with supporting elements.
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public class LoginFrame extends JFrame {

    /**
     * Reference to the data service for accessing repositories and system operations.
     * Used for authentication and loading sample data.
     */
    private final DataService dataService;

    /**
     * Constructs the LoginFrame with all necessary UI components and event handlers.
     * Initializes the authentication service, sets up the window properties,
     * and creates the login form with all interactive elements.
     *
     * @param dataService The data service providing access to repositories and system operations.
     */
    public LoginFrame(DataService dataService) {
        // Store the data service reference for use throughout the frame
        this.dataService = dataService;

        // Initialize validation service for input validation
        ValidationService validationService = new ValidationService();

        // Create authentication service with necessary dependencies
        // This service handles user login and profile management
        AuthService authService = new AuthService(
                dataService.getUserRepository(),
                dataService.getProfileRepository(),
                validationService
        );

        // Configure the main window properties
        setTitle(Constants.APP_TITLE);  // Set window title from constants
        setSize(1080, 760);  // Set initial window size
        setMinimumSize(new Dimension(980, 700));  // Set minimum window size for usability
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Exit application when window is closed
        setLocationRelativeTo(null);  // Center the window on screen
        UiTheme.styleFrame(this);  // Apply custom UI styling to the frame

        // Create form input components
        JTextField usernameField = new JTextField();  // Text field for username/email input
        JPasswordField passwordField = new JPasswordField();  // Password field with hidden characters
        char passwordEchoChar = passwordField.getEchoChar();  // Store original echo character for toggle
        JComboBox<Role> roleBox = new JComboBox<>(Role.values());  // Dropdown for role selection
        JButton loginButton = UiTheme.createPrimaryButton("Login");  // Primary login button
        JButton registerButton = UiTheme.createSecondaryButton("Create TA Account");  // Button to open registration
        JButton loadSampleButton = UiTheme.createSecondaryButton("Load Demo Data");  // Button to load sample data
        JCheckBox showPasswordBox = new JCheckBox("Show Password");  // Checkbox to toggle password visibility

        // Apply consistent UI styling to all input components
        UiTheme.styleTextField(usernameField);
        UiTheme.styleTextField(passwordField);
        UiTheme.styleComboBox(roleBox);
        UiTheme.styleCheckBox(showPasswordBox);

        // Create the main content panel with themed styling
        JPanel root = UiTheme.createPagePanel();

        // Build the hero panel (left side) with system branding and information
        JPanel hero = buildHeroPanel();

        // Create the login card (right side) with form and supporting information
        JPanel loginCard = UiTheme.createCard("Sign In", "Use one of the demo accounts or your registered TA account.");

        // Create the login form grid layout
        JPanel form = UiTheme.createFormGrid();
        // Add form rows with labels and input fields
        UiTheme.addFormRow(form, 0, "Username / Email", usernameField);
        UiTheme.addFormRow(form, 2, "Password", passwordField);
        UiTheme.addFormRow(form, 4, "", showPasswordBox);  // Password visibility toggle
        UiTheme.addFormRow(form, 6, "Role", roleBox);  // Role selection dropdown

        // Create the demo accounts information panel
        JPanel supportPanel = new JPanel();
        supportPanel.setOpaque(false);  // Transparent background
        supportPanel.setLayout(new BoxLayout(supportPanel, BoxLayout.Y_AXIS));  // Vertical layout

        // Demo accounts title label
        JLabel supportTitle = new JLabel("Demo Accounts");
        supportTitle.setForeground(UiTheme.TEXT);  // Use theme text color
        supportTitle.setFont(UiTheme.uiFont(Font.BOLD, 13));  // Bold font for emphasis
        supportTitle.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center alignment

        // Text area displaying demo account credentials
        JTextArea accountArea = new JTextArea(
                "TA: ta1@bupt.edu.cn / ta123\n" +
                        "MO 1: mo1@bupt.edu.cn / mo123\n" +
                        "MO 2: mo2@bupt.edu.cn / mo123\n" +
                        "Admin: admin@bupt.edu.cn / admin123"
        );
        accountArea.setEditable(false);  // Read-only
        accountArea.setOpaque(false);  // Transparent background
        accountArea.setForeground(UiTheme.MUTED_TEXT);  // Muted text color
        accountArea.setFont(UiTheme.uiFont(Font.PLAIN, 13));  // Plain font
        accountArea.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center alignment
        accountArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));  // Limit height

        // Add components to the support panel with spacing
        supportPanel.add(supportTitle);
        supportPanel.add(Box.createVerticalStrut(8));  // Vertical spacing
        supportPanel.add(accountArea);

        // Create the action buttons panel (login and secondary actions)
        JPanel actionRow = new JPanel(new BorderLayout(0, 10));
        actionRow.setOpaque(false);  // Transparent background

        // Panel for secondary action buttons (load sample data and register)
        JPanel secondaryActions = new JPanel(new GridLayout(1, 2, 10, 0));
        secondaryActions.setOpaque(false);  // Transparent background
        secondaryActions.add(loadSampleButton);
        secondaryActions.add(registerButton);

        // Arrange action buttons: secondary actions on top, login button below
        actionRow.add(secondaryActions, BorderLayout.NORTH);
        actionRow.add(loginButton, BorderLayout.SOUTH);

        // Assemble the login card body with form, demo info, and actions
        JPanel cardBody = new JPanel(new BorderLayout(0, 18));
        cardBody.setOpaque(false);  // Transparent background
        cardBody.add(form, BorderLayout.NORTH);  // Form at the top
        cardBody.add(supportPanel, BorderLayout.CENTER);  // Demo accounts in the middle
        cardBody.add(actionRow, BorderLayout.SOUTH);  // Action buttons at the bottom
        loginCard.add(cardBody, BorderLayout.CENTER);

        // Create the main split-pane layout dividing hero and login sections
        JSplitPane layout = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hero, loginCard);
        layout.setResizeWeight(0.5);  // Equal split initially
        UiTheme.styleSplitPane(layout);  // Apply custom styling to the split pane

        // Add event listener for the login button
        loginButton.addActionListener(event -> {
            try {
                // Attempt to authenticate the user with provided credentials
                User user = authService.login(
                        usernameField.getText(),  // Get username from text field
                        new String(passwordField.getPassword()),  // Get password from password field
                        (Role) roleBox.getSelectedItem()  // Get selected role from combo box
                );
                // If login successful, open the appropriate dashboard
                openDashboard(user);
            } catch (Exception ex) {
                // Display error message if login fails
                UiMessage.error(this, ex.getMessage());
            }
        });

        // Add event listener for password visibility toggle
        showPasswordBox.addActionListener(event ->
                // Toggle password field echo character based on checkbox state
                passwordField.setEchoChar(showPasswordBox.isSelected() ? (char) 0 : passwordEchoChar)
        );

        // Add event listener for register button - opens registration frame
        registerButton.addActionListener(event -> new RegisterFrame(authService).setVisible(true));

        // Add event listener for load sample data button
        loadSampleButton.addActionListener(event -> {
            // Load sample data into the system
            dataService.loadSampleData();
            // Display success message with demo account credentials
            UiMessage.info(this, "Sample data loaded.\nTA: ta1@bupt.edu.cn / ta123\nMO 1: mo1@bupt.edu.cn / mo123\nMO 2: mo2@bupt.edu.cn / mo123\nAdmin: admin@bupt.edu.cn / admin123");
        });

        // Add the main layout to the root panel and then to the frame
        root.add(layout, BorderLayout.CENTER);
        add(UiTheme.wrapPage(root), BorderLayout.CENTER);
    }

    /**
     * Builds the hero panel displayed on the left side of the login screen.
     * This panel contains branding information, system description, and key metrics.
     *
     * @return A JPanel containing the hero content with branding and metrics.
     */
    private JPanel buildHeroPanel() {
        // Create the main hero panel with vertical layout
        JPanel hero = new JPanel(new BorderLayout(0, 16));
        hero.setBackground(new Color(18, 52, 86));  // Dark blue background
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(32, 74, 118), 1, true),  // Border color
                BorderFactory.createEmptyBorder(28, 28, 28, 28)  // Padding
        ));

        // Top section with branding and description
        JPanel top = new JPanel();
        top.setOpaque(false);  // Transparent background
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));  // Vertical box layout

        // School badge/eyebrow text
        JLabel eyebrow = UiTheme.createBadge("BUPT International School", new Color(255, 255, 255, 40), Color.WHITE);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);  // Left align

        // Main title with HTML formatting for multi-line display
        JLabel title = new JLabel("<html><div style='width:260px;'>TA Recruitment<br>Management System</div></html>");
        title.setForeground(Color.WHITE);  // White text
        title.setFont(UiTheme.uiFont(Font.BOLD, 30));  // Large bold font
        title.setBorder(BorderFactory.createEmptyBorder(18, 0, 14, 0));  // Spacing

        // System description/summary text
        JLabel summary = new JLabel("<html><div style='width:320px;'>Manage teaching assistant applications, publish module jobs, and balance workloads from one desktop system.</div></html>");
        summary.setForeground(new Color(229, 236, 247));  // Light blue-gray text
        summary.setFont(UiTheme.uiFont(Font.PLAIN, 16));  // Regular font

        // Add branding elements to the top panel
        top.add(eyebrow);
        top.add(title);
        top.add(summary);

        // Bottom section with metrics cards
        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));  // 3 columns, horizontal gap
        metrics.setOpaque(false);  // Transparent background
        metrics.add(buildMetricCard("3", "Roles"));  // Number of user roles
        metrics.add(buildMetricCard("3+", "Demo Jobs"));  // Number of demo job postings
        metrics.add(buildMetricCard("Live", "File Data"));  // Data storage type

        // Assemble the hero panel
        hero.add(top, BorderLayout.CENTER);  // Branding in center
        hero.add(metrics, BorderLayout.SOUTH);  // Metrics at bottom

        return hero;
    }

    /**
     * Builds a metric card component for displaying key statistics in the hero panel.
     * Each card shows a value and label in a styled container.
     *
     * @param value The numeric or text value to display (e.g., "3", "Live").
     * @param label The descriptive label for the metric (e.g., "Roles", "File Data").
     * @return A JPanel representing a styled metric card.
     */
    private JPanel buildMetricCard(String value, String label) {
        // Create the metric card panel
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(new Color(255, 255, 255, 24));  // Semi-transparent white background
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 35), 1, true),  // Light border
                BorderFactory.createEmptyBorder(14, 14, 14, 14)  // Padding
        ));

        // Value label with large, bold font
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);  // White text
        valueLabel.setFont(UiTheme.uiFont(Font.BOLD, 24));  // Large bold font

        // Label text with smaller font
        JLabel labelLabel = new JLabel(label);
        labelLabel.setForeground(new Color(224, 232, 245));  // Light blue-gray text
        labelLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));  // Small plain font

        // Arrange value and label vertically
        panel.add(valueLabel, BorderLayout.CENTER);  // Value in center
        panel.add(labelLabel, BorderLayout.SOUTH);  // Label at bottom

        return panel;
    }

    /**
     * Opens the appropriate dashboard frame based on the user's role after successful login.
     * This method disposes of the login frame and creates the role-specific dashboard.
     *
     * @param user The authenticated user object containing role information.
     * @throws IllegalStateException if the user's role is not supported.
     */
    private void openDashboard(User user) {
        // Determine which dashboard to open based on user role
        switch (user.getRole()) {
            case TA -> {
                // Open TA dashboard for teaching assistants
                new TADashboardFrame(dataService, user).setVisible(true);
            }
            case MO -> {
                // Open MO (Module Organizer) management frame
                new MOManagementFrame(dataService, user).setVisible(true);
            }
            case ADMIN -> {
                // Open admin dashboard for administrators
                new AdminDashboardFrame(dataService, user).setVisible(true);
            }
            default -> {
                // Throw exception for unsupported roles
                throw new IllegalStateException("Unsupported role.");
            }
        }
        // Close the login frame after opening the dashboard
        dispose();
    }
}
