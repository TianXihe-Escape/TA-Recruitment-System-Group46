package util;

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
     * The directory path where all application data files are stored.
     * This path is dynamically resolved based on the project structure to ensure
     * consistent data location whether running from IDE or packaged application.
     *
     * The resolution logic checks if the current working directory is named
     * "TA-Recruitment-System-Group46" and if so, uses the parent directory's "data" folder.
     * Otherwise, it uses a "data" subdirectory of the current working directory.
     *
     * This approach allows the application to work correctly in different deployment scenarios.
     */
    public static final Path DATA_DIR = resolveDataDir();

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
     * Resolves the data directory path based on the current working directory structure.
     * This method implements intelligent path resolution to handle different deployment scenarios:
     *
     * 1. If the current directory is named "TA-Recruitment-System-Group46" (development setup),
     *    it uses the parent directory's "data" folder. This allows the application to share
     *    data with other components or maintain data outside the project directory.
     *
     * 2. Otherwise, it uses a "data" subdirectory within the current working directory.
     *    This is suitable for standalone deployments where all files are contained within
     *    the application directory.
     *
     * The method normalizes all paths to ensure consistent behavior across different
     * operating systems and file system representations.
     *
     * @return The resolved Path to the data directory.
     */
    private static Path resolveDataDir() {
        // Get the current working directory as an absolute, normalized path
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();

        // Attempt to resolve parent data directory for development environment
        Path parentDataDir = workingDirectory.getParent() == null
                ? null  // No parent directory available
                : workingDirectory.getParent().resolve("data").normalize();  // Parent's data directory

        // Check if we should use the parent data directory
        // Condition: parent exists AND current directory name matches expected project name
        if (parentDataDir != null && workingDirectory.getFileName() != null
                && "TA-Recruitment-System-Group46".equals(workingDirectory.getFileName().toString())) {
            return parentDataDir;  // Use parent data directory for development
        }

        // Default: use data subdirectory of current working directory
        return workingDirectory.resolve("data").normalize();
    }
}
