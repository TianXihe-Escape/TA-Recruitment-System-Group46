package ui;

import model.User;
import service.AuthService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Authenticated password-change dialog for the currently signed-in user.
 */
public class ChangePasswordFrame extends JFrame {
    public ChangePasswordFrame(AuthService authService, User currentUser) {
        setTitle("Change Password - " + Constants.APP_TITLE);
        setSize(560, 460);
        setMinimumSize(new Dimension(520, 420));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        JPasswordField oldPasswordField = new JPasswordField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        char oldEchoChar = oldPasswordField.getEchoChar();
        char passwordEchoChar = passwordField.getEchoChar();
        char confirmEchoChar = confirmField.getEchoChar();
        JCheckBox showPasswordsBox = new JCheckBox("Show Passwords");
        JButton saveButton = UiTheme.createPrimaryButton("Change Password");
        JButton cancelButton = UiTheme.createSecondaryButton("Cancel");
        saveButton.setIcon(new SimpleLineIcon(SimpleLineIcon.Type.SAVE, Color.WHITE));
        saveButton.setIconTextGap(8);
        cancelButton.setIcon(new SimpleLineIcon(SimpleLineIcon.Type.LOGOUT, Color.WHITE));
        cancelButton.setIconTextGap(8);

        UiTheme.styleTextField(oldPasswordField);
        UiTheme.styleTextField(passwordField);
        UiTheme.styleTextField(confirmField);
        UiTheme.styleCheckBox(showPasswordsBox);

        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Change Password", "Enter your current password before choosing a new one.");
        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "Old Password", oldPasswordField);
        UiTheme.addFormRow(form, 2, "New Password", passwordField);
        UiTheme.addFormRow(form, 4, "Confirm Password", confirmField);
        UiTheme.addFormRow(form, 6, "", showPasswordsBox);

        showPasswordsBox.addActionListener(event -> {
            boolean show = showPasswordsBox.isSelected();
            oldPasswordField.setEchoChar(show ? (char) 0 : oldEchoChar);
            passwordField.setEchoChar(show ? (char) 0 : passwordEchoChar);
            confirmField.setEchoChar(show ? (char) 0 : confirmEchoChar);
        });
        saveButton.addActionListener(event -> {
            try {
                String newPassword = new String(passwordField.getPassword());
                authService.changePassword(
                        currentUser.getUsername(),
                        new String(oldPasswordField.getPassword()),
                        newPassword,
                        new String(confirmField.getPassword())
                );
                currentUser.setPassword(newPassword);
                UiMessage.info(this, "Password changed successfully.");
                dispose();
            } catch (Exception ex) {
                UiMessage.error(this, ex.getMessage());
            }
        });
        cancelButton.addActionListener(event -> dispose());

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(form, BorderLayout.CENTER);
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, cancelButton, saveButton), BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        root.add(card, BorderLayout.CENTER);
        add(root);
    }
}
