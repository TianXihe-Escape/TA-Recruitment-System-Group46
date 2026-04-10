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
    void shouldExplainAcceptedPhoneFormatsClearly() {
        assertTrue(validationService.validateApplicantProfile("Li Hua", "li@bupt.edu.cn", "138 0000 0000").isEmpty());
        assertEquals(List.of("Please enter an 11-digit Chinese phone number or an international number with 7 to 15 digits."),
                validationService.validateApplicantProfile("Li Hua", "li@bupt.edu.cn", "12345"));
    }

    @Test
    void shouldAcceptChineseNameAndNormalizeSkills() {
        assertTrue(validationService.validateApplicantProfile("李 华", "li@bupt.edu.cn", "13800000000").isEmpty());
        assertEquals(List.of("Java", "沟通", "Python"),
                validationService.parseSkills("Java，沟通、Python; java"));
    }

    @Test
    void shouldRejectDuplicateSkillsIgnoringCase() {
        assertEquals(List.of("Skills cannot contain duplicate entries."),
                validationService.validateSkillInput("Java, java, Python", "Skills", false));
    }

    @Test
    void shouldRejectTooManySkills() {
        assertEquals(List.of("Skills must contain 10 items or fewer."),
                validationService.validateSkillInput("Java, Python, SQL, Git, Docker, Linux, Spring, React, Testing, Scrum, AWS",
                        "Skills",
                        false));
    }

    @Test
    void shouldRejectOverlongRequiredSkill() {
        assertEquals(List.of("Each required skill must be 30 characters or fewer."),
                validationService.validateSkillInput("This skill description is definitely more than thirty characters",
                        "Required skills",
                        true));
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
