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

    @Test
    void shouldHideExpiredOpenJobsFromApplicantSearch() {
        JobPosting available = buildJob("j1", "COMP1001", "Programming Fundamentals", JobCategory.MODULE_TA);
        JobPosting expired = buildJob("j2", "COMP1002", "Expired Vacancy", JobCategory.MODULE_TA);
        expired.setApplicationDeadline(LocalDate.now().minusDays(1));
        JobPosting closed = buildJob("j3", "COMP1003", "Closed Vacancy", JobCategory.MODULE_TA);
        closed.setStatus(JobStatus.CLOSED);
        jobRepository.saveAll(List.of(available, expired, closed));

        List<JobPosting> openJobs = jobService.getOpenJobs();

        assertEquals(1, openJobs.size());
        assertEquals("j1", openJobs.get(0).getJobId());
    }

    @Test
    void shouldCreateReadableJobIdAndPersistNewJob() {
        JobPosting seededDemoStyleJob = buildJob("job-ebu6475", "EBU6475", "Microprocessor Systems Design", JobCategory.MODULE_TA);
        jobRepository.saveAll(List.of(seededDemoStyleJob));

        JobPosting newJob = buildJob("", "EBU6475", "Extra Lab Support", JobCategory.INVIGILATION);
        newJob.setJobType(JobPosting.JOB_TYPE_INVIGILATION);
        newJob.setWorkloadType(JobPosting.WORKLOAD_TYPE_TOTAL);
        newJob.setStartDate(LocalDate.now().plusDays(8).toString());
        newJob.setEndDate(LocalDate.now().plusDays(8).toString());
        newJob.setApplicationDeadline(LocalDate.now().plusDays(7));
        newJob.setPostedBy("user-mo-chao-shu");

        jobService.saveJob(newJob);

        assertEquals("job-ebu6475-01", newJob.getJobId());
        JobPosting savedJob = jobRepository.findById("job-ebu6475-01").orElseThrow();
        assertEquals("Extra Lab Support", savedJob.getModuleTitle());
        assertEquals(JobPosting.JOB_TYPE_INVIGILATION, savedJob.getJobType());
        assertEquals(JobPosting.WORKLOAD_TYPE_TOTAL, savedJob.getWorkloadType());
        assertEquals(2, jobRepository.findAll().size());
    }

    @Test
    void shouldPersistEditedJobFields() {
        JobPosting job = buildJob("job-cbu5201", "CBU5201", "Machine Learning", JobCategory.MODULE_TA);
        jobRepository.saveAll(List.of(job));

        job.setJobType(JobPosting.JOB_TYPE_INVIGILATION);
        job.setWorkloadType(JobPosting.WORKLOAD_TYPE_TOTAL);
        job.setHours(3);
        job.setStartDate(LocalDate.now().plusDays(8).toString());
        job.setEndDate(LocalDate.now().plusDays(8).toString());
        job.setSchedule("15:00-17:00");
        job.setLocation("805");
        job.setApplicationDeadline(LocalDate.now().plusDays(7));
        job.setDuties("Study support.");
        job.setRequiredSkills(List.of("Java"));

        jobService.saveJob(job);

        JobPosting savedJob = jobRepository.findById("job-cbu5201").orElseThrow();
        assertEquals(JobPosting.JOB_TYPE_INVIGILATION, savedJob.getJobType());
        assertEquals(JobPosting.WORKLOAD_TYPE_TOTAL, savedJob.getWorkloadType());
        assertEquals(3, savedJob.getHours());
        assertEquals("805", savedJob.getLocation());
        assertEquals(List.of("Java"), savedJob.getRequiredSkills());
    }

    @Test
    void shouldContinueModuleSpecificJobIds() {
        JobPosting existingCustomJob = buildJob("job-ebu6475-01", "EBU6475", "First Extra Support", JobCategory.INVIGILATION);
        JobPosting otherModuleJob = buildJob("job-ebu6304-01", "EBU6304", "Software Extra Support", JobCategory.INVIGILATION);
        jobRepository.saveAll(List.of(existingCustomJob, otherModuleJob));

        JobPosting newJob = buildJob("", "EBU6475", "Second Extra Support", JobCategory.INVIGILATION);
        newJob.setJobType(JobPosting.JOB_TYPE_INVIGILATION);
        newJob.setWorkloadType(JobPosting.WORKLOAD_TYPE_TOTAL);
        newJob.setStartDate(LocalDate.now().plusDays(8).toString());
        newJob.setEndDate(LocalDate.now().plusDays(8).toString());
        newJob.setApplicationDeadline(LocalDate.now().plusDays(7));

        jobService.saveJob(newJob);

        assertEquals("job-ebu6475-02", newJob.getJobId());
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
