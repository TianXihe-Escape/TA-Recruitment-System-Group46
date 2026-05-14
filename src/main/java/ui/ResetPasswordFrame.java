package ui;

import service.AuthService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Local password reset dialog for registered accounts.
 */
public class ResetPasswordFrame extends JFrame {
    public ResetPasswordFrame(AuthService authService) {
        setTitle("Reset Password - " + Constants.APP_TITLE);
        setSize(560, 420);
        setMinimumSize(new Dimension(520, 400));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        char passwordEchoChar = passwordField.getEchoChar();
        char confirmEchoChar = confirmField.getEchoChar();
        JCheckBox showPasswordsBox = new JCheckBox("Show Passwords");
        JButton resetButton = UiTheme.createPrimaryButton("Reset Password");
        JButton cancelButton = UiTheme.createSecondaryButton("Cancel");

        UiTheme.styleTextField(emailField);
        UiTheme.styleTextField(passwordField);
        UiTheme.styleTextField(confirmField);
        UiTheme.styleCheckBox(showPasswordsBox);

        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Reset Password", "Update the password for an existing local account.");
        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "Email", emailField);
        UiTheme.addFormRow(form, 2, "New Password", passwordField);
        UiTheme.addFormRow(form, 4, "Confirm Password", confirmField);
        UiTheme.addFormRow(form, 6, "", showPasswordsBox);

        showPasswordsBox.addActionListener(event -> {
            boolean show = showPasswordsBox.isSelected();
            passwordField.setEchoChar(show ? (char) 0 : passwordEchoChar);
            confirmField.setEchoChar(show ? (char) 0 : confirmEchoChar);
        });
        resetButton.addActionListener(event -> {
            try {
                authService.resetPassword(
                        emailField.getText(),
                        new String(passwordField.getPassword()),
                        new String(confirmField.getPassword())
                );
                UiMessage.info(this, "Password reset successfully. You can now log in with the new password.");
                dispose();
            } catch (Exception ex) {
                UiMessage.error(this, ex.getMessage());
            }
        });
        cancelButton.addActionListener(event -> dispose());

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(form, BorderLayout.CENTER);
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, cancelButton, resetButton), BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        add(root);
    }
}
