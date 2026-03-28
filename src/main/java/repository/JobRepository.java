package repository;

import com.fasterxml.jackson.core.type.TypeReference;
import model.JobPosting;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Repository for job postings.
 */
public class JobRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public JobRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public List<JobPosting> findAll() {
        return dataStore.readList(filePath, new TypeReference<>() {
        });
    }

    public Optional<JobPosting> findById(String jobId) {
        return findAll().stream()
                .filter(job -> job.getJobId().equals(jobId))
                .findFirst();
    }

    public void saveAll(List<JobPosting> jobs) {
        dataStore.writeList(filePath, jobs);
    }
}
