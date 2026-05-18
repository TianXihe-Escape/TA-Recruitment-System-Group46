package ui;

import model.User;
import service.AuthService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Authenticated password-change dialog for the currently signed-in user.
 *
 * Unlike the admin reset utility, this frame requires the user's current
 * password first. That keeps the workflow aligned with a normal self-service
 * password update rather than an administrator override.
 */
public class ChangePasswordFrame extends JFrame {
    /**
     * Builds the password-change window for the active signed-in user.
     *
     * The frame only collects input and displays feedback. Validation such as
     * checking the old password, enforcing password rules, and confirming the
     * new password match is delegated to AuthService.
     */
    public ChangePasswordFrame(AuthService authService, User currentUser) {
        setTitle("Change Password - " + Constants.APP_TITLE);
        setSize(560, 460);
        setMinimumSize(new Dimension(520, 420));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        // These fields are local to the dialog because their values are only
        // needed during one password-change attempt.
        JPasswordField oldPasswordField = new JPasswordField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        // Preserve the original masking characters so the checkbox can toggle
        // between visible and hidden passwords without losing the theme defaults.
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

        // The dialog reuses the shared card-and-form layout so account utilities
        // keep the same spacing and visual rhythm as the rest of the Swing UI.
        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Change Password", "Enter your current password before choosing a new one.");
        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "Old Password", oldPasswordField);
        UiTheme.addFormRow(form, 2, "New Password", passwordField);
        UiTheme.addFormRow(form, 4, "Confirm Password", confirmField);
        UiTheme.addFormRow(form, 6, "", showPasswordsBox);

        // Toggling all three fields together makes it easier for users to catch
        // typing mistakes when entering or confirming a new password.
        showPasswordsBox.addActionListener(event -> {
            boolean show = showPasswordsBox.isSelected();
            oldPasswordField.setEchoChar(show ? (char) 0 : oldEchoChar);
            passwordField.setEchoChar(show ? (char) 0 : passwordEchoChar);
            confirmField.setEchoChar(show ? (char) 0 : confirmEchoChar);
        });

        // AuthService owns the actual password-change rules. The UI simply passes
        // the current username plus the three password values to that service.
        saveButton.addActionListener(event -> {
            try {
                String newPassword = new String(passwordField.getPassword());
                authService.changePassword(
                        currentUser.getUsername(),
                        new String(oldPasswordField.getPassword()),
                        newPassword,
                        new String(confirmField.getPassword())
                );
                // Keep the in-memory user object consistent with the persisted
                // password so any follow-up UI logic sees the latest state.
                currentUser.setPassword(newPassword);
                UiMessage.info(this, "Password changed successfully.");
                dispose();
            } catch (Exception ex) {
                UiMessage.error(this, ex.getMessage());
            }
        });

        // Cancel simply closes the dialog because there is no draft state that
        // needs to be preserved once the user abandons the password update.
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
