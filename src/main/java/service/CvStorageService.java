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
        return storeDocumentForApplicant(applicantId, sourceCvPath, previousCvPath, Constants.CV_DIR, "");
    }

    public String storeSupportingDocumentForApplicant(String applicantId, String sourcePath, String previousPath) {
        return storeDocumentForApplicant(applicantId, sourcePath, previousPath, Constants.SUPPORTING_DOC_DIR, "-supporting");
    }

    public String storeDocumentForApplicant(String applicantId,
                                            String sourceDocumentPath,
                                            String previousDocumentPath,
                                            Path targetDirectory,
                                            String suffix) {
        if (sourceDocumentPath == null || sourceDocumentPath.isBlank()) {
            deleteManagedDocument(previousDocumentPath, targetDirectory);
            return "";
        }

        Path source = resolveCvPath(sourceDocumentPath);
        if (isManagedDocument(source, targetDirectory)) {
            deleteOldManagedDocument(previousDocumentPath, source, targetDirectory);
            return toProjectRelativePath(source);
        }
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("The selected document file could not be found:\n" + sourceDocumentPath);
        }

        String fileName = source.getFileName().toString();
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("CV file must use a common resume format: PDF, DOC, DOCX, RTF, or TXT.");
        }

        Path destination = targetDirectory.resolve(safeFileName(applicantId) + suffix + "." + extension).normalize();
        try {
            Files.createDirectories(targetDirectory);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            deleteOldManagedDocument(previousDocumentPath, destination, targetDirectory);
            return toProjectRelativePath(destination);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the document file:\n" + e.getMessage(), e);
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
        deleteManagedDocument(cvPath, Constants.CV_DIR);
    }

    public void deleteManagedSupportingDocument(String documentPath) {
        deleteManagedDocument(documentPath, Constants.SUPPORTING_DOC_DIR);
    }

    private void deleteManagedDocument(String documentPath, Path targetDirectory) {
        if (documentPath == null || documentPath.isBlank()) {
            return;
        }
        Path path = resolveCvPath(documentPath);
        if (!isManagedDocument(path, targetDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not delete the old CV file:\n" + e.getMessage(), e);
        }
    }

    private void deleteOldManagedDocument(String previousDocumentPath, Path newDocumentPath, Path targetDirectory) {
        if (previousDocumentPath == null || previousDocumentPath.isBlank()) {
            return;
        }
        Path previous = resolveCvPath(previousDocumentPath);
        if (!previous.equals(newDocumentPath.toAbsolutePath().normalize())) {
            deleteManagedDocument(previousDocumentPath, targetDirectory);
        }
    }

    private boolean isManagedDocument(Path path, Path targetDirectory) {
        return path.toAbsolutePath().normalize().startsWith(targetDirectory.toAbsolutePath().normalize());
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
