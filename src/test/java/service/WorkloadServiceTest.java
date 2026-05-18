package service;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.JobPosting;
import model.WorkloadRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadServiceTest {
    private final WorkloadService workloadService = new WorkloadService();

    @Test
    void shouldFlagOverloadWhenThresholdExceeded() {
        ApplicantProfile profile = new ApplicantProfile("a1", "u1");
        profile.setName("Li Hua");

        JobPosting job = new JobPosting();
        job.setJobId("j1");
        job.setModuleCode("COMP1001");
        job.setModuleTitle("Programming");
        job.setHours(12);

        ApplicationRecord application = new ApplicationRecord();
        application.setApplicantId("a1");
        application.setJobId("j1");
        application.setStatus(ApplicationStatus.ACCEPTED);

        List<WorkloadRecord> records = workloadService.buildWorkloadRecords(
                List.of(profile),
                List.of(job),
                List.of(application),
                10
        );

        assertEquals(12, records.get(0).getTotalHours());
        assertEquals(12, records.get(0).getWeeklyHours());
        assertEquals(0, records.get(0).getOneOffHours());
        assertTrue(records.get(0).isOverload());
    }

    @Test
    void shouldCalculateProjectedOverload() {
        ApplicantProfile profile = new ApplicantProfile("a1", "u1");
        profile.setName("Li Hua");

        JobPosting acceptedJob = new JobPosting();
        acceptedJob.setJobId("j1");
        acceptedJob.setModuleCode("COMP1001");
        acceptedJob.setModuleTitle("Programming");
        acceptedJob.setHours(8);

        ApplicationRecord application = new ApplicationRecord();
        application.setApplicantId("a1");
        application.setJobId("j1");
        application.setStatus(ApplicationStatus.ACCEPTED);

        List<WorkloadRecord> records = workloadService.buildWorkloadRecords(
                List.of(profile),
                List.of(acceptedJob),
                List.of(application),
                10
        );

        JobPosting newJob = new JobPosting();
        newJob.setHours(4);

        assertEquals(12, workloadService.projectedHours("a1", newJob, records));
        assertTrue(workloadService.wouldExceedThreshold("a1", newJob, records, 10));
    }

    @Test
    void shouldSeparateOneOffWorkloadFromWeeklyThreshold() {
        ApplicantProfile profile = new ApplicantProfile("a1", "u1");
        profile.setName("Li Hua");

        JobPosting weeklyJob = new JobPosting();
        weeklyJob.setJobId("j1");
        weeklyJob.setModuleCode("COMP1001");
        weeklyJob.setModuleTitle("Programming");
        weeklyJob.setHours(6);
        weeklyJob.setWorkloadType(JobPosting.WORKLOAD_TYPE_WEEKLY);

        JobPosting eventJob = new JobPosting();
        eventJob.setJobId("j2");
        eventJob.setModuleCode("COMP1001");
        eventJob.setModuleTitle("Final Assessment Invigilation");
        eventJob.setHours(5);
        eventJob.setWorkloadType(JobPosting.WORKLOAD_TYPE_TOTAL);

        ApplicationRecord weeklyApplication = new ApplicationRecord();
        weeklyApplication.setApplicantId("a1");
        weeklyApplication.setJobId("j1");
        weeklyApplication.setStatus(ApplicationStatus.ACCEPTED);

        ApplicationRecord eventApplication = new ApplicationRecord();
        eventApplication.setApplicantId("a1");
        eventApplication.setJobId("j2");
        eventApplication.setStatus(ApplicationStatus.ACCEPTED);

        List<WorkloadRecord> records = workloadService.buildWorkloadRecords(
                List.of(profile),
                List.of(weeklyJob, eventJob),
                List.of(weeklyApplication, eventApplication),
                10
        );

        assertEquals(6, records.get(0).getWeeklyHours());
        assertEquals(5, records.get(0).getOneOffHours());
        assertEquals(6, records.get(0).getTotalHours());
        assertEquals(6, workloadService.projectedHours("a1", eventJob, records));
    }
}
