package service;

import model.JobCategory;
import model.JobPosting;
import model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JobRepository;
import repository.JsonDataStore;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobServiceTest {
    @TempDir
    Path tempDir;

    private JobRepository jobRepository;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        JsonDataStore dataStore = new JsonDataStore();
        jobRepository = new JobRepository(dataStore, tempDir.resolve("jobs.json"));
        jobService = new JobService(jobRepository, new ValidationService());
    }

    @Test
    void shouldSearchAndFilterOpenJobs() {
        JobPosting moduleJob = buildJob("j1", "COMP1001", "Programming Fundamentals", JobCategory.MODULE_TA);
        moduleJob.setRequiredSkills(List.of("Java"));
        JobPosting invigilation = buildJob("j2", "EXAM2026", "Final Exam Invigilation", JobCategory.INVIGILATION);
        invigilation.setRequiredSkills(List.of("Communication"));
        jobRepository.saveAll(List.of(moduleJob, invigilation));

        assertEquals(1, jobService.searchOpenJobs("java", "", null).size());
        assertEquals(1, jobService.searchOpenJobs("", "", JobCategory.INVIGILATION).size());
        assertEquals(1, jobService.searchOpenJobs("", "comp1001", null).size());
    }

    private JobPosting buildJob(String id, String code, String title, JobCategory category) {
        JobPosting job = new JobPosting();
        job.setJobId(id);
        job.setModuleCode(code);
        job.setModuleTitle(title);
        job.setCategory(category);
        job.setSemester("2026 Spring");
        job.setHours(4);
        job.setStatus(JobStatus.OPEN);
        job.setApplicationDeadline(LocalDate.now().plusDays(7));
        job.setDuties("Support delivery.");
        job.setRequiredSkills(List.of("Communication"));
        return job;
    }
}
