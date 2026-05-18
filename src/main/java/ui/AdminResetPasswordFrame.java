package ui;

import service.AuthService;
import ui.dialogs.UiMessage;
import util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Administrator-only reset dialog for TA/MO/Admin local accounts.
 *
 * This frame is intentionally small and task-focused: the admin provides an
 * account email plus a replacement password, and the actual validation and
 * reset rules remain inside AuthService.
 */
public class AdminResetPasswordFrame extends JFrame {
    /**
     * Builds the reset-password window used by administrators.
     *
     * The frame delegates all account checks to AuthService so the Swing layer
     * stays responsible only for collecting input and surfacing feedback.
     */
    public AdminResetPasswordFrame(AuthService authService) {
        setTitle("Admin Reset Password - " + Constants.APP_TITLE);
        setSize(560, 440);
        setMinimumSize(new Dimension(520, 400));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        // These local fields only belong to this one dialog, so they are kept
        // inside the constructor instead of being promoted to frame-level state.
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        // Preserve the theme's default echo characters so "show password" can
        // cleanly restore the masked state after being toggled off again.
        char passwordEchoChar = passwordField.getEchoChar();
        char confirmEchoChar = confirmField.getEchoChar();
        JCheckBox showPasswordsBox = new JCheckBox("Show Passwords");
        JButton resetButton = UiTheme.createPrimaryButton("Reset Password");
        JButton cancelButton = UiTheme.createSecondaryButton("Cancel");
        resetButton.setIcon(new SimpleLineIcon(SimpleLineIcon.Type.SAVE, Color.WHITE));
        resetButton.setIconTextGap(8);
        cancelButton.setIcon(new SimpleLineIcon(SimpleLineIcon.Type.LOGOUT, Color.WHITE));
        cancelButton.setIconTextGap(8);

        UiTheme.styleTextField(emailField);
        UiTheme.styleTextField(passwordField);
        UiTheme.styleTextField(confirmField);
        UiTheme.styleCheckBox(showPasswordsBox);

        // The dialog uses the same themed card-and-form layout as the rest of
        // the application so admin utilities feel like part of one UI system.
        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Admin Reset Password", "Reset a local account password after signing in as Admin.");
        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "Account Email", emailField);
        UiTheme.addFormRow(form, 2, "New Password", passwordField);
        UiTheme.addFormRow(form, 4, "Confirm Password", confirmField);
        UiTheme.addFormRow(form, 6, "", showPasswordsBox);

        // Showing both password fields together reduces mismatch mistakes during
        // manual resets, especially when the admin is reading a temporary password aloud.
        showPasswordsBox.addActionListener(event -> {
            boolean show = showPasswordsBox.isSelected();
            passwordField.setEchoChar(show ? (char) 0 : passwordEchoChar);
            confirmField.setEchoChar(show ? (char) 0 : confirmEchoChar);
        });

        // Submission is intentionally thin here: AuthService owns validation such
        // as account existence, password policy, and confirmation matching.
        resetButton.addActionListener(event -> {
            try {
                authService.resetPassword(
                        emailField.getText(),
                        new String(passwordField.getPassword()),
                        new String(confirmField.getPassword())
                );
                UiMessage.info(this, "Password reset successfully.");
                dispose();
            } catch (Exception ex) {
                UiMessage.error(this, ex.getMessage());
            }
        });

        // Cancel simply closes the utility window because no intermediate state
        // needs to be saved or propagated back to the caller.
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
