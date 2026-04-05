package repository;

import model.ApplicationRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for application records.
 */
public class ApplicationRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public ApplicationRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public List<ApplicationRecord> findAll() {
        return dataStore.readList(filePath, ApplicationRecord.class);
    }

    public List<ApplicationRecord> findByApplicantId(String applicantId) {
        return findAll().stream()
                .filter(record -> record.getApplicantId().equals(applicantId))
                .collect(Collectors.toList());
    }

    public List<ApplicationRecord> findByJobId(String jobId) {
        return findAll().stream()
                .filter(record -> record.getJobId().equals(jobId))
                .collect(Collectors.toList());
    }

    public Optional<ApplicationRecord> findById(String applicationId) {
        return findAll().stream()
                .filter(record -> record.getApplicationId().equals(applicationId))
                .findFirst();
    }

    public void saveAll(List<ApplicationRecord> applications) {
        dataStore.writeList(filePath, applications);
    }
}
