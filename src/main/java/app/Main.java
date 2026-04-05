package app;

import service.DataService;
import ui.LoginFrame;
import ui.UiTheme;

import javax.swing.*;

/**
 * Launches the stand-alone Swing application.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            DataService dataService = new DataService();
            dataService.ensureDataFiles();
            new LoginFrame(dataService).setVisible(true);
        });
    }
}

