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
 * Entry point frame for login and demo data loading.
 */
public class LoginFrame extends JFrame {
    private final DataService dataService;

    public LoginFrame(DataService dataService) {
        this.dataService = dataService;
        ValidationService validationService = new ValidationService();
        AuthService authService = new AuthService(
                dataService.getUserRepository(),
                dataService.getProfileRepository(),
                validationService
        );

        setTitle(Constants.APP_TITLE);
        setSize(1080, 760);
        setMinimumSize(new Dimension(980, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        char passwordEchoChar = passwordField.getEchoChar();
        JComboBox<Role> roleBox = new JComboBox<>(Role.values());
        JButton loginButton = UiTheme.createPrimaryButton("Login");
        JButton registerButton = UiTheme.createSecondaryButton("Create TA Account");
        JButton loadSampleButton = UiTheme.createSecondaryButton("Load Demo Data");
        JCheckBox showPasswordBox = new JCheckBox("Show Password");

        UiTheme.styleTextField(usernameField);
        UiTheme.styleTextField(passwordField);
        UiTheme.styleComboBox(roleBox);
        UiTheme.styleCheckBox(showPasswordBox);

        JPanel root = UiTheme.createPagePanel();
        JPanel hero = buildHeroPanel();
        JPanel loginCard = UiTheme.createCard("Sign In", "Use one of the demo accounts or your registered TA account.");

        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "Username / Email", usernameField);
        UiTheme.addFormRow(form, 2, "Password", passwordField);
        UiTheme.addFormRow(form, 4, "", showPasswordBox);
        UiTheme.addFormRow(form, 6, "Role", roleBox);

        JPanel supportPanel = new JPanel();
        supportPanel.setOpaque(false);
        supportPanel.setLayout(new BoxLayout(supportPanel, BoxLayout.Y_AXIS));
        JLabel supportTitle = new JLabel("Demo Accounts");
        supportTitle.setForeground(UiTheme.TEXT);
        supportTitle.setFont(UiTheme.uiFont(Font.BOLD, 13));
        supportTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextArea accountArea = new JTextArea(
                "TA: ta1@bupt.edu.cn / ta123\n" +
                        "MO 1: mo1@bupt.edu.cn / mo123\n" +
                        "MO 2: mo2@bupt.edu.cn / mo123\n" +
                        "Admin: admin@bupt.edu.cn / admin123"
        );
        accountArea.setEditable(false);
        accountArea.setOpaque(false);
        accountArea.setForeground(UiTheme.MUTED_TEXT);
        accountArea.setFont(UiTheme.uiFont(Font.PLAIN, 13));
        accountArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        accountArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        supportPanel.add(supportTitle);
        supportPanel.add(Box.createVerticalStrut(8));
        supportPanel.add(accountArea);

        JPanel actionRow = new JPanel(new BorderLayout(0, 10));
        actionRow.setOpaque(false);
        JPanel secondaryActions = new JPanel(new GridLayout(1, 2, 10, 0));
        secondaryActions.setOpaque(false);
        secondaryActions.add(loadSampleButton);
        secondaryActions.add(registerButton);
        actionRow.add(secondaryActions, BorderLayout.NORTH);
        actionRow.add(loginButton, BorderLayout.SOUTH);

        JPanel cardBody = new JPanel(new BorderLayout(0, 18));
        cardBody.setOpaque(false);
        cardBody.add(form, BorderLayout.NORTH);
        cardBody.add(supportPanel, BorderLayout.CENTER);
        cardBody.add(actionRow, BorderLayout.SOUTH);
        loginCard.add(cardBody, BorderLayout.CENTER);

        JSplitPane layout = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hero, loginCard);
        layout.setResizeWeight(0.5);
        UiTheme.styleSplitPane(layout);

        loginButton.addActionListener(event -> {
            try {
                User user = authService.login(
                        usernameField.getText(),
                        new String(passwordField.getPassword()),
                        (Role) roleBox.getSelectedItem()
                );
                openDashboard(user);
            } catch (Exception ex) {
                UiMessage.error(this, ex.getMessage());
            }
        });
        showPasswordBox.addActionListener(event ->
                passwordField.setEchoChar(showPasswordBox.isSelected() ? (char) 0 : passwordEchoChar));

        registerButton.addActionListener(event -> new RegisterFrame(authService).setVisible(true));
        loadSampleButton.addActionListener(event -> {
            dataService.loadSampleData();
            UiMessage.info(this, "Sample data loaded.\nTA: ta1@bupt.edu.cn / ta123\nMO 1: mo1@bupt.edu.cn / mo123\nMO 2: mo2@bupt.edu.cn / mo123\nAdmin: admin@bupt.edu.cn / admin123");
        });

        root.add(layout, BorderLayout.CENTER);
        add(UiTheme.wrapPage(root), BorderLayout.CENTER);
    }

    private JPanel buildHeroPanel() {
        JPanel hero = new JPanel(new BorderLayout(0, 16));
        hero.setBackground(new Color(18, 52, 86));
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(32, 74, 118), 1, true),
                BorderFactory.createEmptyBorder(28, 28, 28, 28)
        ));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel eyebrow = UiTheme.createBadge("BUPT iSchool", new Color(255, 255, 255, 40), Color.WHITE);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("<html><div style='width:260px;'>TA Recruitment<br>Management System</div></html>");
        title.setForeground(Color.WHITE);
        title.setFont(UiTheme.uiFont(Font.BOLD, 30));
        title.setBorder(BorderFactory.createEmptyBorder(18, 0, 14, 0));

        JLabel summary = new JLabel("<html><div style='width:320px;'>Manage teaching assistant applications, publish module jobs, and balance workloads from one desktop system.</div></html>");
        summary.setForeground(new Color(229, 236, 247));
        summary.setFont(UiTheme.uiFont(Font.PLAIN, 16));

        top.add(eyebrow);
        top.add(title);
        top.add(summary);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.add(buildMetricCard("3", "Roles"));
        metrics.add(buildMetricCard("3+", "Demo Jobs"));
        metrics.add(buildMetricCard("Live", "File Data"));

        hero.add(top, BorderLayout.CENTER);
        hero.add(metrics, BorderLayout.SOUTH);
        return hero;
    }

    private JPanel buildMetricCard(String value, String label) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(new Color(255, 255, 255, 24));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 35), 1, true),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(UiTheme.uiFont(Font.BOLD, 24));

        JLabel labelLabel = new JLabel(label);
        labelLabel.setForeground(new Color(224, 232, 245));
        labelLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));

        panel.add(valueLabel, BorderLayout.CENTER);
        panel.add(labelLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void openDashboard(User user) {
        switch (user.getRole()) {
            case TA -> new TADashboardFrame(dataService, user).setVisible(true);
            case MO -> new MOManagementFrame(dataService, user).setVisible(true);
            case ADMIN -> new AdminDashboardFrame(dataService, user).setVisible(true);
            default -> throw new IllegalStateException("Unsupported role.");
        }
        dispose();
    }
}
