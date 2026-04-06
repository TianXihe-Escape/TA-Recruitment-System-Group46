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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shouldAllowReapplyByOverwritingRejectedApplication() {
        ApplicantProfile profile = buildProfile();
        JobPosting job = buildJob();
        jobRepository.saveAll(List.of(job));

        ApplicationRecord rejected = new ApplicationRecord();
        rejected.setApplicationId("apply-01");
        rejected.setApplicantId("a1");
        rejected.setJobId("j1");
        rejected.setStatus(ApplicationStatus.REJECTED);
        rejected.setReviewerNotes("missing experience");
        rejected.setMatchScore(20);
        rejected.setMissingSkills(List.of("Java"));
        applicationRepository.saveAll(new ArrayList<>(List.of(rejected)));

        ApplicationRecord reapplied = applicationService.apply(profile, job);
        List<ApplicationRecord> records = applicationRepository.findAll();

        assertEquals(1, records.size());
        assertEquals("apply-01", reapplied.getApplicationId());
        assertEquals(ApplicationStatus.SUBMITTED, reapplied.getStatus());
        assertEquals(ApplicationStatus.SUBMITTED, records.get(0).getStatus());
        assertEquals(100, records.get(0).getMatchScore());
        assertEquals(0, records.get(0).getMissingSkills().size());
        assertNull(records.get(0).getReviewerNotes());
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

    @Test
    void shouldGenerateNextSequentialApplicationId() {
        ApplicantProfile profile = buildProfile();
        profile.setApplicantId("a2");

        JobPosting existingJob = buildJob();
        JobPosting nextJob = buildJob();
        nextJob.setJobId("j2");
        nextJob.setModuleCode("COMP1002");
        nextJob.setModuleTitle("Data Structures");
        jobRepository.saveAll(List.of(existingJob, nextJob));

        ApplicationRecord first = new ApplicationRecord();
        first.setApplicationId("apply-01");
        first.setApplicantId("a1");
        first.setJobId("j1");

        ApplicationRecord second = new ApplicationRecord();
        second.setApplicationId("apply-03");
        second.setApplicantId("a3");
        second.setJobId("j3");

        ApplicationRecord legacy = new ApplicationRecord();
        legacy.setApplicationId("apply-3e4142ef");
        legacy.setApplicantId("a4");
        legacy.setJobId("j4");

        applicationRepository.saveAll(new ArrayList<>(List.of(first, second, legacy)));

        ApplicationRecord created = applicationService.apply(profile, nextJob);

        assertEquals("apply-04", created.getApplicationId());
    }

    @Test
    void shouldReopenJobByClearingAcceptedApplication() {
        ApplicationRecord record = new ApplicationRecord();
        record.setApplicationId("x1");
        record.setApplicantId("a1");
        record.setJobId("j1");
        record.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepository.saveAll(new ArrayList<>(List.of(record)));

        applicationService.reopenJob("j1");

        List<ApplicationRecord> records = applicationRepository.findAll();
        assertEquals(ApplicationStatus.ACCEPTED, records.get(0).getStatus());
    }

    @Test
    void shouldKeepJobOpenUntilRequiredTaCountIsFilled() {
        ApplicationRecord first = new ApplicationRecord();
        first.setApplicationId("x1");
        first.setApplicantId("a1");
        first.setJobId("j1");
        first.setStatus(ApplicationStatus.SHORTLISTED);

        ApplicationRecord second = new ApplicationRecord();
        second.setApplicationId("x2");
        second.setApplicantId("a2");
        second.setJobId("j1");
        second.setStatus(ApplicationStatus.SHORTLISTED);

        applicationRepository.saveAll(new ArrayList<>(List.of(first, second)));
        JobPosting job = buildJob();
        job.setRequiredTaCount(2);
        jobRepository.saveAll(List.of(job));

        applicationService.updateStatus("x2", ApplicationStatus.ACCEPTED, "first accepted");

        JobPosting updatedJob = jobRepository.findById("j1").orElseThrow();
        assertEquals(JobStatus.OPEN, updatedJob.getStatus());
    }

    @Test
    void shouldCloseJobWhenRequiredTaCountIsFilled() {
        ApplicationRecord first = new ApplicationRecord();
        first.setApplicationId("x1");
        first.setApplicantId("a1");
        first.setJobId("j1");
        first.setStatus(ApplicationStatus.ACCEPTED);

        ApplicationRecord second = new ApplicationRecord();
        second.setApplicationId("x2");
        second.setApplicantId("a2");
        second.setJobId("j1");
        second.setStatus(ApplicationStatus.SHORTLISTED);

        applicationRepository.saveAll(new ArrayList<>(List.of(first, second)));

        JobPosting job = buildJob();
        job.setRequiredTaCount(2);
        job.setStatus(JobStatus.OPEN);
        jobRepository.saveAll(List.of(job));

        applicationService.updateStatus("x2", ApplicationStatus.ACCEPTED, "second accepted");

        JobPosting updatedJob = jobRepository.findById("j1").orElseThrow();
        assertEquals(JobStatus.CLOSED, updatedJob.getStatus());
    }

    @Test
    void shouldCancelAcceptedApplicationAndReopenJobWhenBelowRequiredTaCount() {
        ApplicationRecord record = new ApplicationRecord();
        record.setApplicationId("x1");
        record.setApplicantId("a1");
        record.setJobId("j1");
        record.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepository.saveAll(new ArrayList<>(List.of(record)));

        JobPosting job = buildJob();
        job.setRequiredTaCount(2);
        job.setStatus(JobStatus.CLOSED);
        jobRepository.saveAll(List.of(job));

        applicationService.cancelAcceptance("x1", "MO cancelled the offer");

        ApplicationRecord updatedRecord = applicationRepository.findAll().get(0);
        JobPosting updatedJob = jobRepository.findById("j1").orElseThrow();

        assertEquals(ApplicationStatus.SHORTLISTED, updatedRecord.getStatus());
        assertTrue(updatedRecord.getReviewerNotes().contains("MO cancelled the offer"));
        assertEquals(JobStatus.OPEN, updatedJob.getStatus());
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
