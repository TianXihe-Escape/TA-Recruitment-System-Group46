package util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Shared configuration values and file locations.
 */
public final class Constants {
    public static final Path DATA_DIR = resolveDataDir();
    public static final Path USERS_FILE = DATA_DIR.resolve("users.json");
    public static final Path PROFILES_FILE = DATA_DIR.resolve("profiles.json");
    public static final Path JOBS_FILE = DATA_DIR.resolve("jobs.json");
    public static final Path APPLICATIONS_FILE = DATA_DIR.resolve("applications.json");
    public static final Path CONFIG_FILE = DATA_DIR.resolve("config.json");
    public static final String APP_TITLE = "BUPT International School TA Recruitment System";

    private Constants() {
    }

    private static Path resolveDataDir() {
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        Path parentDataDir = workingDirectory.getParent() == null
                ? null
                : workingDirectory.getParent().resolve("data").normalize();
        if (parentDataDir != null && workingDirectory.getFileName() != null
                && "TA-Recruitment-System-Group46".equals(workingDirectory.getFileName().toString())) {
            return parentDataDir;
        }
        return workingDirectory.resolve("data").normalize();
    }
}
