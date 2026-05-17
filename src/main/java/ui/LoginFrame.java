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
        setSize(760, 720);  // Set initial window size
        setMinimumSize(new Dimension(620, 640));  // Set minimum window size for usability
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Exit application when window is closed
        setLocationRelativeTo(null);  // Center the window on screen
        UiTheme.styleFrame(this);  // Apply custom UI styling to the frame

        // Create form input components
        JTextField usernameField = new JTextField();  // Text field for username/email input
        JPasswordField passwordField = new JPasswordField();  // Password field with hidden characters
        char passwordEchoChar = passwordField.getEchoChar();  // Store original echo character for toggle
        JComboBox<Role> roleBox = new JComboBox<>(Role.values());  // Dropdown for role selection
        JButton loginButton = UiTheme.createPrimaryButton("Login");  // Primary login button
        JButton registerButton = createTextActionButton("Create TA Account");  // Button to open registration
        JButton loadSampleButton = createTextActionButton("Load Demo Data");  // Button to load sample data
        JCheckBox showPasswordBox = new JCheckBox("Show Password");  // Checkbox to toggle password visibility

        // Apply consistent UI styling to all input components
        UiTheme.styleTextField(usernameField);
        UiTheme.styleTextField(passwordField);
        UiTheme.styleComboBox(roleBox);
        UiTheme.styleCheckBox(showPasswordBox);

        // Create the main content panel with themed styling
        JPanel root = buildLoginRoot();

        // Create the login form grid layout
        JPanel form = UiTheme.createFormGrid();
        // Add form rows with labels and input fields
        UiTheme.addFormRow(form, 0, "Username / Email", usernameField);
        UiTheme.addFormRow(form, 2, "Password", passwordField);
        UiTheme.addFormRow(form, 4, "", showPasswordBox);  // Password visibility toggle
        UiTheme.addFormRow(form, 6, "Role", roleBox);  // Role selection dropdown

        // Create the action buttons panel (login and secondary actions)
        JPanel actionRow = new JPanel(new BorderLayout(0, 16));
        actionRow.setOpaque(false);  // Transparent background

        // Panel for secondary action buttons (load sample data and register)
        JPanel secondaryActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        secondaryActions.setOpaque(false);  // Transparent background
        secondaryActions.add(registerButton);
        secondaryActions.add(loadSampleButton);

        // Arrange action buttons: primary login first, secondary actions below
        actionRow.add(loginButton, BorderLayout.NORTH);
        actionRow.add(secondaryActions, BorderLayout.SOUTH);

        // Assemble the login card body with form, demo info, and actions
        JPanel cardBody = new JPanel(new BorderLayout(0, 22));
        cardBody.setOpaque(false);  // Transparent background

        JPanel mainStack = new JPanel();
        mainStack.setOpaque(false);
        mainStack.setLayout(new BoxLayout(mainStack, BoxLayout.Y_AXIS));
        mainStack.add(buildBrandHeader());
        mainStack.add(form);
        cardBody.add(mainStack, BorderLayout.CENTER);
        cardBody.add(actionRow, BorderLayout.SOUTH);  // Action buttons at the bottom
        JPanel loginCard = createCenteredLoginCard();
        loginCard.add(cardBody, BorderLayout.CENTER);

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
            UiMessage.info(this,
                    "Sample data loaded.\n\n"
                            + "Demo accounts:\n"
                            + "TA: alice.chen@demo.local / Password123\n"
                            + "MO: ling.ma@qmul.ac.uk / Password123\n"
                            + "Admin: admin@bupt.edu.cn / admin123");
        });

        // Add the main login card to the root panel and then to the frame
        root.add(loginCard, new GridBagConstraints());
        add(root, BorderLayout.CENTER);
    }

    private JPanel buildLoginRoot() {
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint paint = new GradientPaint(
                        0, 0, new Color(230, 241, 250),
                        getWidth(), getHeight(), new Color(248, 250, 253)
                );
                g.setPaint(paint);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(36, 99, 235, 18));
                g.fillOval(-120, -100, 320, 320);
                g.setColor(new Color(20, 184, 166, 16));
                g.fillOval(getWidth() - 180, getHeight() - 180, 280, 280);
                g.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
        return root;
    }

    private JPanel createCenteredLoginCard() {
        JPanel card = new JPanel(new BorderLayout(0, 22));
        card.setBackground(new Color(255, 255, 255, 238));
        card.setPreferredSize(new Dimension(430, 520));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(212, 222, 236), 1, true),
                BorderFactory.createEmptyBorder(34, 36, 30, 36)
        ));
        return card;
    }

    private JPanel buildBrandHeader() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel school = new JLabel("BUPT International School");
        school.setAlignmentX(Component.CENTER_ALIGNMENT);
        school.setForeground(UiTheme.MUTED_TEXT);
        school.setFont(UiTheme.uiFont(Font.BOLD, 13));

        JLabel title = new JLabel("TA Recruitment");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.uiFont(Font.BOLD, 28));

        JLabel subtitle = new JLabel("Management System");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setForeground(UiTheme.TEXT);
        subtitle.setFont(UiTheme.uiFont(Font.BOLD, 22));

        panel.add(school);
        panel.add(Box.createVerticalStrut(12));
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(28));
        return panel;
    }

    private JButton createTextActionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.uiFont(Font.PLAIN, 13));
        button.setForeground(UiTheme.PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
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
