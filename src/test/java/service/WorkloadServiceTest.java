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
        assertTrue(records.get(0).isOverload());
    }
}
