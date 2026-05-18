package service;

import model.Role;
import model.User;
import model.ApplicantProfile;
import model.JobCategory;
import model.JobPosting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonDataStore;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonDataStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndReadList() {
        JsonDataStore store = new JsonDataStore();
        Path file = tempDir.resolve("users.json");
        List<User> users = List.of(new User("u1", "admin@bupt.edu.cn", "admin123", Role.ADMIN));

        store.writeList(file, users);
        List<User> loaded = store.readList(file, User.class);

        assertEquals(1, loaded.size());
        assertEquals("admin@bupt.edu.cn", loaded.get(0).getUsername());
    }

    @Test
    void shouldPersistExtendedProfileFields() {
        JsonDataStore store = new JsonDataStore();
        Path file = tempDir.resolve("profiles.json");
        ApplicantProfile profile = new ApplicantProfile("a1", "u1");
        profile.setProgramme("Software Engineering");
        profile.setYearOfStudy("Year 3");
        profile.setSupportingDocumentPaths(List.of(
                "supporting-documents/a1-award.pdf",
                "supporting-documents/a1-proof.pdf"
        ));
        profile.setFavoriteJobIds(List.of("j1", "j2"));

        store.writeList(file, List.of(profile));
        List<ApplicantProfile> loaded = store.readList(file, ApplicantProfile.class);

        assertEquals("Software Engineering", loaded.get(0).getProgramme());
        assertEquals("supporting-documents/a1-award.pdf", loaded.get(0).getSupportingDocumentPath());
        assertEquals(List.of("supporting-documents/a1-award.pdf", "supporting-documents/a1-proof.pdf"),
                loaded.get(0).getSupportingDocumentPaths());
        assertEquals(List.of("j1", "j2"), loaded.get(0).getFavoriteJobIds());
    }

    @Test
    void shouldReadLegacySingleSupportingDocumentPath() throws Exception {
        JsonDataStore store = new JsonDataStore();
        Path file = tempDir.resolve("profiles.json");
        Files.writeString(file, """
                [
                  {
                    "applicantId": "a1",
                    "userId": "u1",
                    "supportingDocumentPath": "supporting-documents/a1-supporting.pdf",
                    "favoriteJobIds": []
                  }
                ]
                """);

        List<ApplicantProfile> loaded = store.readList(file, ApplicantProfile.class);

        assertEquals("supporting-documents/a1-supporting.pdf", loaded.get(0).getSupportingDocumentPath());
        assertEquals(List.of("supporting-documents/a1-supporting.pdf"), loaded.get(0).getSupportingDocumentPaths());
    }

    @Test
    void shouldPersistJobCategoryAndSemester() {
        JsonDataStore store = new JsonDataStore();
        Path file = tempDir.resolve("jobs.json");
        JobPosting job = new JobPosting();
        job.setJobId("j1");
        job.setCategory(JobCategory.INVIGILATION);
        job.setSemester("2026 Spring");

        store.writeList(file, List.of(job));
        List<JobPosting> loaded = store.readList(file, JobPosting.class);

        assertEquals(JobCategory.INVIGILATION, loaded.get(0).getCategory());
        assertEquals("2026 Spring", loaded.get(0).getSemester());
    }

    @Test
    void shouldDefaultMissingJobScheduleAndWorkloadFieldsForOldJson() throws Exception {
        JsonDataStore store = new JsonDataStore();
        Path file = tempDir.resolve("jobs.json");
        Files.writeString(file, """
                [
                  {
                    "jobId": "j-old",
                    "moduleCode": "COMP1001",
                    "moduleTitle": "Programming",
                    "category": "MODULE_TA",
                    "semester": "2026 Spring",
                    "duties": "Support labs",
                    "hours": 6,
                    "requiredTaCount": 1,
                    "requiredSkills": ["Java"],
                    "applicationDeadline": null,
                    "status": "OPEN",
                    "postedBy": "mo1"
                  }
                ]
                """);

        List<JobPosting> loaded = store.readList(file, JobPosting.class);

        assertEquals(JobPosting.JOB_TYPE_COURSE_SUPPORT, loaded.get(0).getJobType());
        assertEquals(JobPosting.WORKLOAD_TYPE_WEEKLY, loaded.get(0).getWorkloadType());
        assertEquals("", loaded.get(0).getStartDate());
        assertEquals("", loaded.get(0).getEndDate());
        assertEquals("", loaded.get(0).getSchedule());
        assertEquals("", loaded.get(0).getLocation());
    }
}
