package service;

import util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stores applicant CV files inside the project so reviewers read a stable copy.
 */
public class CvStorageService {
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx", "rtf", "txt");
    private static final long MAX_DOCUMENT_SIZE_BYTES = 5L * 1024L * 1024L;

    public String storeCvForApplicant(String applicantId, String sourceCvPath, String previousCvPath) {
        return storeDocumentForApplicant(applicantId, sourceCvPath, previousCvPath, Constants.CV_DIR, "");
    }

    public String storeSupportingDocumentForApplicant(String applicantId, String sourcePath, String previousPath) {
        List<String> storedPaths = storeSupportingDocumentsForApplicant(
                applicantId,
                sourcePath == null || sourcePath.isBlank() ? List.of() : List.of(sourcePath),
                previousPath == null || previousPath.isBlank() ? List.of() : List.of(previousPath)
        );
        return storedPaths.isEmpty() ? "" : storedPaths.get(0);
    }

    public List<String> storeSupportingDocumentsForApplicant(String applicantId,
                                                             List<String> sourcePaths,
                                                             List<String> previousPaths) {
        List<String> normalizedSources = new ArrayList<>();
        if (sourcePaths != null) {
            for (String sourcePath : sourcePaths) {
                if (sourcePath != null && !sourcePath.trim().isBlank()) {
                    normalizedSources.add(sourcePath.trim());
                }
            }
        }

        if (normalizedSources.isEmpty()) {
            deleteManagedDocuments(previousPaths, Constants.SUPPORTING_DOC_DIR);
            return List.of();
        }

        List<String> storedPaths = new ArrayList<>();
        int sequence = 1;
        for (String sourcePath : normalizedSources) {
            String suffix = "-supporting-" + String.format("%02d", sequence++);
            storedPaths.add(storeManagedDocumentCopy(applicantId, sourcePath, Constants.SUPPORTING_DOC_DIR, suffix));
        }
        deleteOldManagedDocuments(previousPaths, storedPaths, Constants.SUPPORTING_DOC_DIR);
        return storedPaths;
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

        String storedPath = storeManagedDocumentCopy(applicantId, sourceDocumentPath, targetDirectory, suffix);
        Path destination = resolveCvPath(storedPath);
        deleteOldManagedDocument(previousDocumentPath, destination, targetDirectory);
        return storedPath;
    }

    private String storeManagedDocumentCopy(String applicantId,
                                            String sourceDocumentPath,
                                            Path targetDirectory,
                                            String suffix) {
        Path source = resolveCvPath(sourceDocumentPath);
        if (isManagedDocument(source, targetDirectory)) {
            return toProjectRelativePath(source);
        }
        validateSourceDocument(source, sourceDocumentPath);

        String extension = extensionOf(source.getFileName().toString());
        Path destination = targetDirectory.resolve(safeFileName(applicantId) + suffix + "." + extension).normalize();
        try {
            Files.createDirectories(targetDirectory);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return toProjectRelativePath(destination);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the document file:\n" + e.getMessage(), e);
        }
    }

    private void validateSourceDocument(Path source, String sourceDocumentPath) {
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("The selected document file could not be found:\n" + sourceDocumentPath);
        }
        try {
            if (Files.size(source) > MAX_DOCUMENT_SIZE_BYTES) {
                throw new IllegalArgumentException("The selected document file is too large. Please choose a file up to 5 MB.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not inspect the selected document file:\n" + e.getMessage(), e);
        }

        String extension = extensionOf(source.getFileName().toString());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("CV file must use a common resume format: PDF, DOC, DOCX, RTF, or TXT.");
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

    public void deleteManagedSupportingDocuments(List<String> documentPaths) {
        deleteManagedDocuments(documentPaths, Constants.SUPPORTING_DOC_DIR);
    }

    private void deleteManagedDocuments(List<String> documentPaths, Path targetDirectory) {
        if (documentPaths == null) {
            return;
        }
        for (String documentPath : documentPaths) {
            deleteManagedDocument(documentPath, targetDirectory);
        }
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

    private void deleteOldManagedDocuments(List<String> previousDocumentPaths,
                                           List<String> newDocumentPaths,
                                           Path targetDirectory) {
        if (previousDocumentPaths == null || previousDocumentPaths.isEmpty()) {
            return;
        }
        List<Path> newPaths = new ArrayList<>();
        for (String newDocumentPath : newDocumentPaths) {
            if (newDocumentPath != null && !newDocumentPath.isBlank()) {
                newPaths.add(resolveCvPath(newDocumentPath).toAbsolutePath().normalize());
            }
        }
        for (String previousDocumentPath : previousDocumentPaths) {
            if (previousDocumentPath == null || previousDocumentPath.isBlank()) {
                continue;
            }
            Path previous = resolveCvPath(previousDocumentPath).toAbsolutePath().normalize();
            if (!newPaths.contains(previous)) {
                deleteManagedDocument(previousDocumentPath, targetDirectory);
            }
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
