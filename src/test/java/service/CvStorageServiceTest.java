package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void shouldStoreAnyNumberOfSupportingDocumentsInManagedFolder() throws Exception {
        Path award = tempDir.resolve("award.pdf");
        Path proof = tempDir.resolve("proof.pdf");
        Path transcript = tempDir.resolve("transcript.pdf");
        Path evidence = tempDir.resolve("evidence.pdf");
        Files.writeString(award, "award");
        Files.writeString(proof, "proof");
        Files.writeString(transcript, "transcript");
        Files.writeString(evidence, "evidence");

        List<String> storedPaths = cvStorageService.storeSupportingDocumentsForApplicant(
                "applicant-01",
                List.of(award.toString(), proof.toString(), transcript.toString(), evidence.toString()),
                List.of()
        );

        assertEquals(4, storedPaths.size());
        for (int i = 0; i < storedPaths.size(); i++) {
            String expectedPrefix = "supporting-documents/applicant-01-supporting-" + String.format("%02d", i + 1);
            assertTrue(storedPaths.get(i).replace('\\', '/').startsWith(expectedPrefix));
            assertTrue(Files.isRegularFile(cvStorageService.resolveCvPath(storedPaths.get(i))));
        }

        for (String storedPath : storedPaths) {
            Files.deleteIfExists(cvStorageService.resolveCvPath(storedPath));
        }
    }
}
