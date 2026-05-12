package service;

import util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * Stores applicant CV files inside the project so reviewers read a stable copy.
 */
public class CvStorageService {
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx", "rtf", "txt");

    public String storeCvForApplicant(String applicantId, String sourceCvPath, String previousCvPath) {
        if (sourceCvPath == null || sourceCvPath.isBlank()) {
            deleteManagedCv(previousCvPath);
            return "";
        }

        Path source = resolveCvPath(sourceCvPath);
        if (isManagedCv(source)) {
            deleteOldManagedCv(previousCvPath, source);
            return toProjectRelativePath(source);
        }
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("The selected CV file could not be found:\n" + sourceCvPath);
        }

        String fileName = source.getFileName().toString();
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("CV file must use a common resume format: PDF, DOC, DOCX, RTF, or TXT.");
        }

        Path destination = Constants.CV_DIR.resolve(safeFileName(applicantId) + "." + extension).normalize();
        try {
            Files.createDirectories(Constants.CV_DIR);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            deleteOldManagedCv(previousCvPath, destination);
            return toProjectRelativePath(destination);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the CV file:\n" + e.getMessage(), e);
        }
    }

    public Path resolveCvPath(String cvPath) {
        Path path = Path.of(cvPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Constants.PROJECT_DIR.resolve(path).normalize();
    }

    public void deleteManagedCv(String cvPath) {
        if (cvPath == null || cvPath.isBlank()) {
            return;
        }
        Path path = resolveCvPath(cvPath);
        if (!isManagedCv(path)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete the old CV file:\n" + e.getMessage(), e);
        }
    }

    private void deleteOldManagedCv(String previousCvPath, Path newCvPath) {
        if (previousCvPath == null || previousCvPath.isBlank()) {
            return;
        }
        Path previous = resolveCvPath(previousCvPath);
        if (!previous.equals(newCvPath.toAbsolutePath().normalize())) {
            deleteManagedCv(previousCvPath);
        }
    }

    private boolean isManagedCv(Path path) {
        return path.toAbsolutePath().normalize().startsWith(Constants.CV_DIR.toAbsolutePath().normalize());
    }

    private String toProjectRelativePath(Path path) {
        return Constants.PROJECT_DIR.toAbsolutePath().normalize()
                .relativize(path.toAbsolutePath().normalize())
                .toString();
    }

    private String extensionOf(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String value) {
        String safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "cv" : safe;
    }
}
