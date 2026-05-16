package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CvStorageServiceTest {
    @TempDir
    Path tempDir;

    private final CvStorageService cvStorageService = new CvStorageService();

    @Test
    void shouldRejectUnsupportedCvExtension() throws Exception {
        Path file = tempDir.resolve("cv.png");
        Files.writeString(file, "not a supported document");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cvStorageService.storeCvForApplicant("applicant-01", file.toString(), ""));

        assertTrue(exception.getMessage().contains("common resume format"));
    }

    @Test
    void shouldRejectCvLargerThanFiveMb() throws Exception {
        Path file = tempDir.resolve("large-cv.pdf");
        Files.write(file, new byte[(5 * 1024 * 1024) + 1]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cvStorageService.storeCvForApplicant("applicant-01", file.toString(), ""));

        assertTrue(exception.getMessage().contains("5 MB"));
    }
}
