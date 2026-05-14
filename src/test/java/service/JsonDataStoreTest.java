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
        profile.setSupportingDocumentPath("supporting-documents/a1-supporting.pdf");
        profile.setFavoriteJobIds(List.of("j1", "j2"));

        store.writeList(file, List.of(profile));
        List<ApplicantProfile> loaded = store.readList(file, ApplicantProfile.class);

        assertEquals("Software Engineering", loaded.get(0).getProgramme());
        assertEquals(List.of("j1", "j2"), loaded.get(0).getFavoriteJobIds());
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
}
