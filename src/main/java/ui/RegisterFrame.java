package ui;

import model.User;
import service.AuthService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * TA self-registration screen.
 */
public class RegisterFrame extends JFrame {
    public RegisterFrame(AuthService authService) {
        setTitle("TA Registration - " + Constants.APP_TITLE);
        setSize(420, 260);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        JButton registerButton = new JButton("Register TA Account");

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        form.add(new JLabel("Email"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(new JLabel("Confirm Password"));
        form.add(confirmField);
        form.add(new JLabel());
        form.add(registerButton);

        registerButton.addActionListener(event -> {
            try {
                User user = authService.registerTa(
                        usernameField.getText(),
                        new String(passwordField.getPassword()),
                        new String(confirmField.getPassword())
                );
                UiMessage.info(this, "Registration successful for " + user.getUsername() + ". You can now log in as TA.");
                dispose();
            } catch (Exception ex) {
                UiMessage.error(this, ex.getMessage());
            }
        });

        add(form);
    }
}
