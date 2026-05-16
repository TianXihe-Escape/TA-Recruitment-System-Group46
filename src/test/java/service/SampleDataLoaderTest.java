package service;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.JobPosting;
import model.User;
import model.WorkloadRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.AllocationRepository;
import repository.ApplicantProfileRepository;
import repository.ApplicationRepository;
import repository.ConfigRepository;
import repository.JobRepository;
import repository.JsonDataStore;
import repository.MessageRepository;
import repository.NotificationRepository;
import repository.UserRepository;
import util.SampleDataLoader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleDataLoaderTest {
    @TempDir
    Path tempDir;

    private UserRepository userRepository;
    private ApplicantProfileRepository profileRepository;
    private JobRepository jobRepository;
    private ApplicationRepository applicationRepository;
    private NotificationRepository notificationRepository;
    private MessageRepository messageRepository;
    private AllocationRepository allocationRepository;
    private ConfigRepository configRepository;

    @BeforeEach
    void setUp() {
        JsonDataStore dataStore = new JsonDataStore();
        userRepository = new UserRepository(dataStore, tempDir.resolve("users.json"));
        profileRepository = new ApplicantProfileRepository(dataStore, tempDir.resolve("profiles.json"));
        jobRepository = new JobRepository(dataStore, tempDir.resolve("jobs.json"));
        applicationRepository = new ApplicationRepository(dataStore, tempDir.resolve("applications.json"));
        notificationRepository = new NotificationRepository(dataStore, tempDir.resolve("notifications.json"));
        messageRepository = new MessageRepository(dataStore, tempDir.resolve("messages.json"));
        allocationRepository = new AllocationRepository(dataStore, tempDir.resolve("allocations.json"));
        configRepository = new ConfigRepository(dataStore, tempDir.resolve("config.json"));

        new SampleDataLoader(
                userRepository,
                profileRepository,
                jobRepository,
                applicationRepository,
                notificationRepository,
                messageRepository,
                allocationRepository,
                configRepository
        ).loadSampleData();
    }

    @Test
    void shouldNotCreateDanglingSampleReferences() {
        Set<String> userIds = userRepository.findAll().stream().map(User::getUserId).collect(Collectors.toSet());
        Set<String> profileIds = profileRepository.findAll().stream().map(ApplicantProfile::getApplicantId).collect(Collectors.toSet());
        Set<String> jobIds = jobRepository.findAll().stream().map(JobPosting::getJobId).collect(Collectors.toSet());
        Set<String> applicationIds = applicationRepository.findAll().stream().map(ApplicationRecord::getApplicationId).collect(Collectors.toSet());

        assertTrue(jobRepository.findAll().stream().allMatch(job -> userIds.contains(job.getPostedBy())));
        assertTrue(applicationRepository.findAll().stream()
                .allMatch(application -> profileIds.contains(application.getApplicantId()) && jobIds.contains(application.getJobId())));
        assertTrue(messageRepository.findAll().stream()
                .allMatch(message -> userIds.contains(message.getSenderUserId())
                        && userIds.contains(message.getRecipientUserId())
                        && jobIds.contains(message.getJobId())
                        && applicationIds.contains(message.getApplicationId())));
        assertTrue(notificationRepository.findAll().stream().allMatch(notification -> userIds.contains(notification.getUserId())));
        assertTrue(allocationRepository.findAll().stream()
                .allMatch(allocation -> profileIds.contains(allocation.getApplicantId())
                        && jobIds.contains(allocation.getJobId())
                        && applicationIds.contains(allocation.getApplicationId())));
    }

    @Test
    void shouldIncludeRequestedCoursesAndMoAccounts() {
        assertCourseOwner("EBU6304", "Dr Ling Ma", "ling.ma@qmul.ac.uk");
        assertCourseOwner("EBU6475", "Dr Chao Shu", "chao.shu@qmul.ac.uk");
        assertCourseOwner("EBU6366", "Dr Jin Zhang", "jin.zhang@qmul.ac.uk");
        assertCourseOwner("EBU5606", "Dr Nickos Paltalidis", "n.paltalidis@qmul.ac.uk");
        assertCourseOwner("EBU5042", "Dr Paula Fonseca", "paula.fonseca@qmul.ac.uk");
        assertCourseOwner("CBU5201", "Dr Chao Liu", "chao.liu@qmul.ac.uk");
        assertCourseOwner("EBU6335", "Dr Athen Ma", "a.ma@qmul.ac.uk");

        assertEquals(9, jobRepository.findAll().size());
        assertEquals(Set.of("EBU6304", "EBU6475", "EBU6366", "EBU5606", "EBU5042", "CBU5201", "EBU6335"),
                jobRepository.findAll().stream().map(JobPosting::getModuleCode).collect(Collectors.toSet()));

        AuthService authService = new AuthService(userRepository, profileRepository, new ValidationService());
        assertDoesNotThrow(() -> authService.login("ling.ma@qmul.ac.uk", "Password123", model.Role.MO));
        assertDoesNotThrow(() -> authService.login("chao.shu@qmul.ac.uk", "Password123", model.Role.MO));
        assertDoesNotThrow(() -> authService.login("jin.zhang@qmul.ac.uk", "Password123", model.Role.MO));
        assertDoesNotThrow(() -> authService.login("n.paltalidis@qmul.ac.uk", "Password123", model.Role.MO));
        assertDoesNotThrow(() -> authService.login("paula.fonseca@qmul.ac.uk", "Password123", model.Role.MO));
        assertDoesNotThrow(() -> authService.login("chao.liu@qmul.ac.uk", "Password123", model.Role.MO));
        assertDoesNotThrow(() -> authService.login("a.ma@qmul.ac.uk", "Password123", model.Role.MO));
    }

    @Test
    void shouldIncludeTenDemoTasWithSkillsAndSelfEvaluation() {
        List<String> demoEmails = List.of(
                "alice.chen@demo.local",
                "ben.wang@demo.local",
                "chloe.li@demo.local",
                "daniel.zhang@demo.local",
                "emma.liu@demo.local",
                "frank.zhao@demo.local",
                "grace.xu@demo.local",
                "henry.sun@demo.local",
                "ivy.huang@demo.local",
                "jason.wu@demo.local"
        );
        List<User> users = userRepository.findAll();
        List<ApplicantProfile> profiles = profileRepository.findAll();

        assertEquals(10, demoEmails.size());
        assertEquals(10, profiles.size());
        for (String email : demoEmails) {
            User user = users.stream().filter(item -> email.equals(item.getUsername())).findFirst().orElseThrow();
            ApplicantProfile profile = profiles.stream().filter(item -> user.getUserId().equals(item.getUserId())).findFirst().orElseThrow();

            assertFalse(profile.getSkills().isEmpty());
            assertFalse(profile.getExperienceSummary().isBlank());
            assertTrue(profile.getEmail().endsWith("@demo.local"));
            assertTrue(profile.getCvPath().startsWith("cv/"));
            assertTrue(profile.getCvPath().endsWith(".docx"));
        }
    }

    @Test
    void shouldPersistMoEvaluationInReviewerNotes() {
        ApplicationRecord alice = applicationRepository.findById("apply-01").orElseThrow();
        ApplicationRecord emma = applicationRepository.findById("apply-05").orElseThrow();

        assertTrue(applicationRepository.findAll().stream().allMatch(application ->
                application.getReviewerNotes() != null && !application.getReviewerNotes().isBlank()));
        assertTrue(alice.getReviewerNotes().contains("Strong match for software engineering labs"));
        assertTrue(emma.getReviewerNotes().contains("missing Java, GitHub, Testing and Agile Development"));
    }

    @Test
    void shouldProvideExpectedMatchingDemoCases() {
        assertMatch("applicant-alice", "job-ebu6304", 100);
        assertMatch("applicant-ben", "job-ebu6475", 100);
        assertMatch("applicant-chloe", "job-ebu6366", 80);
        assertMatch("applicant-daniel", "job-ebu5606", 100);
        assertMatch("applicant-grace", "job-ebu5042", 100);
        assertMatch("applicant-henry", "job-cbu5201", 100);
        assertMatch("applicant-ivy", "job-ebu6335", 100);
        assertMatch("applicant-jason", "job-ebu5606", 40);

        ApplicationRecord emma = applicationRepository.findById("apply-05").orElseThrow();
        assertTrue(emma.getMissingSkills().contains("Java"));
        assertTrue(emma.getMissingSkills().contains("Agile Development"));
        assertTrue(emma.getMissingSkills().contains("GitHub"));
        assertTrue(emma.getMissingSkills().contains("Testing"));

        ApplicationRecord jason = applicationRepository.findById("apply-12").orElseThrow();
        assertTrue(jason.getMissingSkills().contains("Microwave Engineering"));
        assertTrue(jason.getMissingSkills().contains("RF Systems"));

        MatchingService matchingService = new MatchingService();
        ApplicantProfile emmaProfile = profileRepository.findByApplicantId("applicant-emma").orElseThrow();
        JobPosting ebu6304 = jobRepository.findById("job-ebu6304").orElseThrow();
        String emmaExplanation = matchingService.calculateMatch(emmaProfile.getSkills(), ebu6304.getRequiredSkills()).getExplanation();
        assertTrue(emmaExplanation.contains("Matched skills:"));
        assertTrue(emmaExplanation.contains("Missing skills:"));
        assertTrue(emmaExplanation.contains("Score formula:"));

        ApplicantProfile jasonProfile = profileRepository.findByApplicantId("applicant-jason").orElseThrow();
        JobPosting ebu6366 = jobRepository.findById("job-ebu6366").orElseThrow();
        String jasonExplanation = matchingService.calculateMatch(jasonProfile.getSkills(), ebu6366.getRequiredSkills()).getExplanation();
        assertTrue(jasonExplanation.contains("Missing skills:"));
        assertTrue(jasonExplanation.contains("Microwave Engineering"));
    }

    @Test
    void shouldBuildWorkloadWarningForAcceptedDemoApplication() {
        WorkloadService workloadService = new WorkloadService();
        assertEquals(10, configRepository.load().getWorkloadThreshold());

        List<WorkloadRecord> records = workloadService.buildWorkloadRecords(
                profileRepository.findAll(),
                jobRepository.findAll(),
                applicationRepository.findAll(),
                configRepository.load().getWorkloadThreshold()
        );

        long frankAcceptedApplications = applicationRepository.findAll().stream()
                .filter(application -> "applicant-frank".equals(application.getApplicantId()))
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        long frankAllocations = allocationRepository.findAll().stream()
                .filter(allocation -> "applicant-frank".equals(allocation.getApplicantId()))
                .filter(allocation -> allocation.isActive())
                .count();
        WorkloadRecord frank = records.stream()
                .filter(record -> "applicant-frank".equals(record.getApplicantId()))
                .findFirst()
                .orElseThrow();

        assertEquals(2, frankAcceptedApplications);
        assertEquals(2, frankAllocations);
        assertEquals(11, frank.getWeeklyHours());
        assertEquals(11, frank.getTotalHours());
        assertEquals(0, frank.getOneOffHours());
        assertTrue(frank.isOverload());
        assertTrue(records.stream().anyMatch(record -> !"applicant-frank".equals(record.getApplicantId()) && !record.isOverload()));

        WorkloadRecord alice = records.stream()
                .filter(record -> "applicant-alice".equals(record.getApplicantId()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, alice.getWeeklyHours());
        assertEquals(3, alice.getOneOffHours());
        assertFalse(alice.isOverload());

        WorkloadRecord jason = records.stream()
                .filter(record -> "applicant-jason".equals(record.getApplicantId()))
                .findFirst()
                .orElseThrow();
        JobPosting ebu6366 = jobRepository.findById("job-ebu6366").orElseThrow();
        assertEquals(4, jason.getWeeklyHours());
        assertEquals(4, jason.getOneOffHours());
        assertEquals(9, workloadService.projectedHours("applicant-jason", ebu6366, records));
    }

    @Test
    void shouldIncludeScheduleAndWorkloadTypesInSampleJobs() {
        List<JobPosting> jobs = jobRepository.findAll();

        assertEquals(7, jobs.stream()
                .filter(job -> JobPosting.WORKLOAD_TYPE_WEEKLY.equals(job.getWorkloadType()))
                .count());
        assertEquals(2, jobs.stream()
                .filter(job -> JobPosting.WORKLOAD_TYPE_TOTAL.equals(job.getWorkloadType()))
                .count());
        assertTrue(jobs.stream().allMatch(job -> !job.getStartDate().isBlank()
                && !job.getEndDate().isBlank()
                && !job.getSchedule().isBlank()
                && !job.getLocation().isBlank()));

        JobPosting invigilation = jobRepository.findById("job-ebu6304-invigilation").orElseThrow();
        JobPosting demo = jobRepository.findById("job-ebu6304-demo").orElseThrow();
        assertEquals(JobPosting.JOB_TYPE_INVIGILATION, invigilation.getJobType());
        assertEquals(JobPosting.JOB_TYPE_DEMO_SUPPORT, demo.getJobType());
        assertEquals(10, configRepository.load().getWorkloadThreshold());
    }

    @Test
    void shouldResolveApplicationReviewOverviewDataForAdmin() {
        List<User> users = userRepository.findAll();
        List<ApplicantProfile> profiles = profileRepository.findAll();
        List<JobPosting> jobs = jobRepository.findAll();
        List<ApplicationRecord> applications = applicationRepository.findAll();

        for (ApplicationRecord application : applications) {
            ApplicantProfile profile = profiles.stream()
                    .filter(item -> application.getApplicantId().equals(item.getApplicantId()))
                    .findFirst()
                    .orElseThrow();
            JobPosting job = jobs.stream()
                    .filter(item -> application.getJobId().equals(item.getJobId()))
                    .findFirst()
                    .orElseThrow();
            User mo = users.stream()
                    .filter(item -> job.getPostedBy().equals(item.getUserId()))
                    .findFirst()
                    .orElseThrow();

            assertFalse(profile.getName().isBlank());
            assertFalse(job.getModuleCode().isBlank());
            assertFalse(job.getModuleTitle().isBlank());
            assertFalse(mo.getName().isBlank());
            assertTrue(application.getStatus() != null);
            assertFalse(application.getReviewerNotes() == null || application.getReviewerNotes().isBlank());
        }

        ApplicationRecord alice = applicationRepository.findById("apply-01").orElseThrow();
        assertTrue(alice.getReviewerNotes().contains("Strong match for software engineering labs"));

        ApplicationRecord emma = applicationRepository.findById("apply-05").orElseThrow();
        assertTrue(emma.getMissingSkills().contains("Java"));
        assertTrue(emma.getMissingSkills().contains("GitHub"));

        ApplicationRecord jason = applicationRepository.findById("apply-12").orElseThrow();
        assertTrue(jason.getMissingSkills().contains("Microwave Engineering"));
        assertTrue(jason.getMissingSkills().contains("RF Systems"));
    }

    @Test
    void shouldCoverExpectedApplicationStatuses() {
        Set<ApplicationStatus> statuses = applicationRepository.findAll().stream()
                .map(ApplicationRecord::getStatus)
                .collect(Collectors.toSet());

        assertTrue(statuses.contains(ApplicationStatus.SUBMITTED));
        assertTrue(statuses.contains(ApplicationStatus.SHORTLISTED));
        assertTrue(statuses.contains(ApplicationStatus.INTERVIEW_INVITED));
        assertTrue(statuses.contains(ApplicationStatus.ACCEPTED));
        assertTrue(statuses.contains(ApplicationStatus.REJECTED));
        assertEquals(15, applicationRepository.findAll().size());
    }

    @Test
    void shouldRemovePreviouslyUncertainDemoMos() {
        Set<String> usernames = userRepository.findAll().stream().map(User::getUsername).collect(Collectors.toSet());

        assertFalse(usernames.contains("mo1@bupt.edu.cn"));
        assertFalse(usernames.contains("mo2@bupt.edu.cn"));
        assertFalse(usernames.contains("gareth.tyson@qmul.ac.uk"));
    }

    private void assertCourseOwner(String moduleCode, String expectedName, String expectedEmail) {
        JobPosting job = jobRepository.findAll().stream()
                .filter(item -> moduleCode.equals(item.getModuleCode()))
                .findFirst()
                .orElseThrow();
        User owner = userRepository.findAll().stream()
                .filter(item -> job.getPostedBy().equals(item.getUserId()))
                .findFirst()
                .orElseThrow();

        assertEquals(expectedName, owner.getName());
        assertEquals(expectedEmail, owner.getUsername());
    }

    private void assertMatch(String applicantId, String jobId, int expectedMinimumScore) {
        ApplicationRecord application = applicationRepository.findAll().stream()
                .filter(item -> applicantId.equals(item.getApplicantId()) && jobId.equals(item.getJobId()))
                .findFirst()
                .orElseThrow();

        assertTrue(application.getMatchScore() >= expectedMinimumScore);
    }
}
