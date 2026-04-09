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
        setSize(560, 520);
        setMinimumSize(new Dimension(520, 500));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        UiTheme.styleFrame(this);

        JTextField nameField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        char passwordEchoChar = passwordField.getEchoChar();
        char confirmEchoChar = confirmField.getEchoChar();
        JButton registerButton = UiTheme.createPrimaryButton("Register TA Account");
        JButton closeButton = UiTheme.createSecondaryButton("Cancel");
        JCheckBox showPasswordsBox = new JCheckBox("Show Passwords");

        UiTheme.styleTextField(nameField);
        UiTheme.styleTextField(usernameField);
        UiTheme.styleTextField(passwordField);
        UiTheme.styleTextField(confirmField);
        UiTheme.styleCheckBox(showPasswordsBox);

        JPanel root = UiTheme.createPagePanel();
        JPanel card = UiTheme.createCard("Create TA Account", "This creates a TA login and an empty applicant profile.");
        JPanel form = UiTheme.createFormGrid();
        UiTheme.addFormRow(form, 0, "Name", nameField);
        UiTheme.addFormRow(form, 2, "Email", usernameField);
        UiTheme.addFormRow(form, 4, "Password", passwordField);
        UiTheme.addFormRow(form, 6, "Confirm Password", confirmField);
        UiTheme.addFormRow(form, 8, "", showPasswordsBox);

        JTextArea note = new JTextArea("After registration, sign in as TA and complete your profile before applying to jobs.");
        note.setEditable(false);
        note.setOpaque(false);
        note.setWrapStyleWord(true);
        note.setLineWrap(true);
        note.setForeground(UiTheme.MUTED_TEXT);
        note.setFont(UiTheme.uiFont(Font.PLAIN, 13));

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(form, BorderLayout.NORTH);
        body.add(note, BorderLayout.CENTER);
        body.add(UiTheme.createButtonRow(FlowLayout.RIGHT, closeButton, registerButton), BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);

        registerButton.addActionListener(event -> {
            try {
                User user = authService.registerTa(
                        usernameField.getText(),
                        nameField.getText(),
                        new String(passwordField.getPassword()),
                        new String(confirmField.getPassword())
                );
                UiMessage.info(this, "Registration successful for " + user.getUsername() + ". You can now log in as TA.");
                dispose();
            } catch (Exception ex) {
                UiMessage.error(this, ex.getMessage());
            }
        });
        showPasswordsBox.addActionListener(event -> {
            boolean showPasswords = showPasswordsBox.isSelected();
            passwordField.setEchoChar(showPasswords ? (char) 0 : passwordEchoChar);
            confirmField.setEchoChar(showPasswords ? (char) 0 : confirmEchoChar);
        });
        closeButton.addActionListener(event -> dispose());

        root.add(card, BorderLayout.CENTER);
        add(root);
    }
}
