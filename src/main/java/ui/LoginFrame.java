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
        setSize(480, 320);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<Role> roleBox = new JComboBox<>(Role.values());
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("TA Register");
        JButton loadSampleButton = new JButton("Load Sample Data");

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        form.add(new JLabel("Username / Email"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(new JLabel("Role"));
        form.add(roleBox);
        form.add(registerButton);
        form.add(loginButton);
        form.add(new JLabel("Demo Support"));
        form.add(loadSampleButton);

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

        registerButton.addActionListener(event -> new RegisterFrame(authService).setVisible(true));
        loadSampleButton.addActionListener(event -> {
            dataService.loadSampleData();
            UiMessage.info(this, "Sample data loaded.\nTA: ta1@bupt.edu.cn / ta123\nMO: mo1@bupt.edu.cn / mo123\nAdmin: admin@bupt.edu.cn / admin123");
        });

        add(form, BorderLayout.CENTER);
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
