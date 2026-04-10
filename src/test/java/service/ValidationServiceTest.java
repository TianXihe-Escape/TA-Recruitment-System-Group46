package service;

import model.JobPosting;
import model.JobStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationServiceTest {
    private final ValidationService validationService = new ValidationService();

    @Test
    void shouldRejectInvalidEmailRegistration() {
        assertFalse(validationService.validateRegistration("not-an-email", "1234", "1234").isEmpty());
    }

    @Test
    void shouldAcceptValidProfileFields() {
        assertTrue(validationService.validateApplicantProfile("Li Hua", "li@bupt.edu.cn", "13800000000").isEmpty());
    }

    @Test
    void shouldRejectInvalidPhoneNumber() {
        assertFalse(validationService.validateApplicantProfile("Li Hua", "li@bupt.edu.cn", "12-abc").isEmpty());
    }

    @Test
    void shouldAcceptChineseNameAndNormalizeSkills() {
        assertTrue(validationService.validateApplicantProfile("\u674E\u534E", "li@bupt.edu.cn", "13800000000").isEmpty());
        assertEquals(List.of("Java", "\u6C9F\u901A", "Python", "SQL"),
                validationService.parseSkills("Java\uFF0C\u6C9F\u901A\u3001Python; java\nSQL"));
    }

    @Test
    void shouldRejectPastDeadlineJobPosting() {
        JobPosting job = new JobPosting();
        job.setModuleCode("comp1001");
        job.setModuleTitle("Intro to Programming");
        job.setHours(6);
        job.setStatus(JobStatus.OPEN);
        job.setApplicationDeadline(LocalDate.now().minusDays(1));
        job.setRequiredSkills(List.of("Java"));
        job.setDuties("Support labs.");

        assertFalse(validationService.validateJobPosting(job).isEmpty());
    }
}
