package repository;

import com.fasterxml.jackson.core.type.TypeReference;
import model.ApplicantProfile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Repository for applicant profiles.
 */
public class ApplicantProfileRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public ApplicantProfileRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public List<ApplicantProfile> findAll() {
        return dataStore.readList(filePath, new TypeReference<>() {
        });
    }

    public Optional<ApplicantProfile> findByUserId(String userId) {
        return findAll().stream()
                .filter(profile -> profile.getUserId().equals(userId))
                .findFirst();
    }

    public Optional<ApplicantProfile> findByApplicantId(String applicantId) {
        return findAll().stream()
                .filter(profile -> profile.getApplicantId().equals(applicantId))
                .findFirst();
    }

    public void saveAll(List<ApplicantProfile> profiles) {
        dataStore.writeList(filePath, profiles);
    }
}

