package repository;

import model.AllocationRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for accepted applicant allocation records.
 */
public class AllocationRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public AllocationRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public List<AllocationRecord> findAll() {
        return dataStore.readList(filePath, AllocationRecord.class);
    }

    public Optional<AllocationRecord> findActiveByApplicationId(String applicationId) {
        return findAll().stream()
                .filter(allocation -> applicationId != null && applicationId.equals(allocation.getApplicationId()))
                .filter(AllocationRecord::isActive)
                .findFirst();
    }

    public List<AllocationRecord> findActiveByApplicantId(String applicantId) {
        return findAll().stream()
                .filter(allocation -> applicantId != null && applicantId.equals(allocation.getApplicantId()))
                .filter(AllocationRecord::isActive)
                .collect(Collectors.toList());
    }

    public void saveAll(List<AllocationRecord> allocations) {
        dataStore.writeList(filePath, allocations);
    }
}
