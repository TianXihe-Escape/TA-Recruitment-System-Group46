package app;

import service.DataService;
import ui.LoginFrame;
import ui.UiTheme;

import javax.swing.*;

/**
 * The main entry point for the TA Recruitment System application.
 * This class is responsible for initializing and launching the Swing-based GUI application.
 * It ensures that the UI is created and displayed on the Event Dispatch Thread (EDT) for thread safety.
 * The application is designed to manage Teaching Assistant recruitment processes, including user authentication,
 * job postings, applications, and matching functionalities.
 *
 * Key responsibilities:
 * - Initialize the UI theme for a consistent user interface.
 * - Set up data services for handling application data.
 * - Ensure data files are present and accessible.
 * - Launch the login screen to start user interaction.
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public class Main {

    /**
     * The main method that serves as the entry point for the Java application.
     * This method is called when the program starts and is responsible for bootstrapping the entire application.
     * It performs the following steps in order:
     * 1. Schedules the GUI initialization on the Event Dispatch Thread using SwingUtilities.invokeLater.
     * 2. Installs the custom UI theme to ensure a uniform appearance across the application.
     * 3. Creates a DataService instance to manage data persistence and retrieval.
     * 4. Ensures that all required data files exist, creating them if necessary.
     * 5. Instantiates and displays the LoginFrame, which is the first screen users see.
     *
     * This approach ensures thread safety for Swing components and proper initialization of application resources.
     *
     * @param args An array of command-line arguments passed to the application.
     *             Currently, no arguments are processed, but this parameter is included for future extensibility.
     */
    public static void main(String[] args) {
        // Ensure all GUI-related operations are performed on the Event Dispatch Thread (EDT)
        // This is crucial for Swing applications to prevent threading issues and ensure responsiveness
        SwingUtilities.invokeLater(() -> {
            // Apply the custom UI theme to give the application a consistent and professional look
            // This might include custom fonts, colors, and component styles
            UiTheme.install();

            // Initialize the data service layer, which handles all data operations
            // This includes reading from and writing to JSON files for persistence
            DataService dataService = new DataService();

            // Verify that all necessary data files are present in the file system
            // If any files are missing, this method will create them with default content
            dataService.ensureDataFiles();

            // Create the main login window and make it visible to the user
            // The LoginFrame constructor takes the dataService to enable user authentication
            // setVisible(true) displays the frame on screen, starting the user interaction
            new LoginFrame(dataService).setVisible(true);
        });
    }
}

