package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized repository for all application-wide constants and configuration values.
 * This class provides a single source of truth for file paths, application metadata,
 * and other immutable configuration settings used throughout the TA Recruitment System.
 *
 * Key benefits of this approach:
 * - Eliminates magic strings and hardcoded values scattered throughout the codebase
 * - Provides a single location for configuration changes
 * - Improves code maintainability and reduces errors from typos
 * - Enables easy configuration management and potential future externalization
 *
 * The constants are organized into logical groups:
 * - File system paths for data storage
 * - Application metadata
 *
 * All constants are declared as public static final to ensure they are:
 * - Accessible from any part of the application
 * - Immutable (cannot be changed at runtime)
 * - Clearly identified as constants by convention
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public final class Constants {

    /**
     * The project directory used as the base for local application storage.
     */
    public static final Path PROJECT_DIR = resolveProjectDir();

    /**
     * The directory path where all application data files are stored.
     * This path is dynamically resolved based on the project structure to ensure
     * consistent data location whether running from IDE or packaged application.
     */
    public static final Path DATA_DIR = PROJECT_DIR.resolve("data").normalize();

    /**
     * The directory path where uploaded CV copies are stored for MO review.
     */
    public static final Path CV_DIR = PROJECT_DIR.resolve("cv").normalize();

    /**
     * File path for storing user account information.
     * Contains all registered users with their authentication credentials,
     * personal details, and role assignments.
     * Data is stored in JSON format for easy serialization and human readability.
     */
    public static final Path USERS_FILE = DATA_DIR.resolve("users.json");

    /**
     * File path for storing applicant profile information.
     * Contains detailed profiles of students applying for TA positions,
     * including academic background, skills, experience, and contact information.
     */
    public static final Path PROFILES_FILE = DATA_DIR.resolve("profiles.json");

    /**
     * File path for storing job posting information.
     * Contains all available TA positions with their requirements,
     * descriptions, deadlines, and associated module information.
     */
    public static final Path JOBS_FILE = DATA_DIR.resolve("jobs.json");

    /**
     * File path for storing job application records.
     * Tracks which applicants have applied for which positions,
     * including application status, timestamps, and review notes.
     */
    public static final Path APPLICATIONS_FILE = DATA_DIR.resolve("applications.json");

    /**
     * File path for storing system configuration settings.
     * Contains application-wide preferences, default values,
     * and configurable parameters that affect system behavior.
     */
    public static final Path CONFIG_FILE = DATA_DIR.resolve("config.json");

    /**
     * The display title for the application window.
     * Used as the title bar text in the main application window and
     * other UI components that display the application name.
     * Includes the institution name for branding and identification.
     */
    public static final String APP_TITLE = "BUPT International School TA Recruitment System";

    /**
     * Private constructor to prevent instantiation.
     * This class contains only static constants and utility methods,
     * so instantiation is not needed and should be prevented.
     * Making the constructor private ensures this utility class
     * follows the singleton pattern for constant classes.
     */
    private Constants() {
        // Utility class - no instantiation allowed
    }

    /**
     * Resolves the application root based on the current working directory.
     *
     * @return The resolved Path to the project directory.
     */
    private static Path resolveProjectDir() {
        // Get the current working directory as an absolute, normalized path
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        String projectDirectoryName = "TA-Recruitment-System-Group46";

        Path current = workingDirectory;
        while (current != null) {
            if (current.getFileName() != null && projectDirectoryName.equals(current.getFileName().toString())) {
                return current.normalize();
            }
            current = current.getParent();
        }

        Path projectDirectory = workingDirectory.resolve(projectDirectoryName);
        if (Files.isDirectory(projectDirectory)) {
            return projectDirectory.normalize();
        }

        // Default: use the current working directory as the application root.
        return workingDirectory;
    }
}
