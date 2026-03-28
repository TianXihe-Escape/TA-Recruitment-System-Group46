package service;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.JobPosting;
import model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.ApplicationRepository;
import repository.JobRepository;
import repository.JsonDataStore;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationServiceTest {
    @TempDir
    Path tempDir;

    private ApplicationRepository applicationRepository;
    private JobRepository jobRepository;
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        JsonDataStore dataStore = new JsonDataStore();
        applicationRepository = new ApplicationRepository(dataStore, tempDir.resolve("applications.json"));
        jobRepository = new JobRepository(dataStore, tempDir.resolve("jobs.json"));
        applicationService = new ApplicationService(applicationRepository, jobRepository, new MatchingService());
    }

    @Test
    void shouldRejectDuplicateApplication() {
        ApplicantProfile profile = buildProfile();
        JobPosting job = buildJob();
        jobRepository.saveAll(List.of(job));

        ApplicationRecord existing = new ApplicationRecord();
        existing.setApplicationId("x1");
        existing.setApplicantId("a1");
        existing.setJobId("j1");
        applicationRepository.saveAll(List.of(existing));

        assertThrows(IllegalStateException.class, () -> applicationService.apply(profile, job));
    }

    @Test
    void shouldRejectFinalizedStatusChange() {
        ApplicationRecord record = new ApplicationRecord();
        record.setApplicationId("x1");
        record.setApplicantId("a1");
        record.setJobId("j1");
        record.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepository.saveAll(List.of(record));
        jobRepository.saveAll(List.of(buildJob()));

        assertThrows(IllegalStateException.class,
                () -> applicationService.updateStatus("x1", ApplicationStatus.REJECTED, "late change"));
    }

    @Test
    void shouldUpdateStatusToShortlisted() {
        ApplicationRecord record = new ApplicationRecord();
        record.setApplicationId("x1");
        record.setApplicantId("a1");
        record.setJobId("j1");
        record.setStatus(ApplicationStatus.SUBMITTED);
        applicationRepository.saveAll(new ArrayList<>(List.of(record)));
        jobRepository.saveAll(List.of(buildJob()));

        applicationService.updateStatus("x1", ApplicationStatus.SHORTLISTED, "strong interview");
        List<ApplicationRecord> records = applicationRepository.findAll();

        assertEquals(ApplicationStatus.SHORTLISTED, records.get(0).getStatus());
    }

    private ApplicantProfile buildProfile() {
        ApplicantProfile profile = new ApplicantProfile("a1", "u1");
        profile.setCvPath("cv.pdf");
        profile.setSkills(List.of("Java"));
        return profile;
    }

    private JobPosting buildJob() {
        JobPosting job = new JobPosting();
        job.setJobId("j1");
        job.setModuleCode("COMP1001");
        job.setModuleTitle("Programming");
        job.setHours(6);
        job.setStatus(JobStatus.OPEN);
        job.setApplicationDeadline(LocalDate.now().plusDays(5));
        job.setRequiredSkills(List.of("Java"));
        return job;
    }
}
