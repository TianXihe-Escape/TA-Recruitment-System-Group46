package util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small file helper methods used by the UI.
 */
public final class FileUtil {
    private FileUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean exists(String path) {
        if (isBlank(path)) {
            return false;
        }
        return Files.exists(Path.of(path));
    }
}
