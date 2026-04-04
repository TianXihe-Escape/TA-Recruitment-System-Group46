package util;

import model.*;
import repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads deterministic demo data for viva use.
 */
public class SampleDataLoader {
    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ConfigRepository configRepository;

    public SampleDataLoader(UserRepository userRepository,
                            ApplicantProfileRepository profileRepository,
                            JobRepository jobRepository,
                            ApplicationRepository applicationRepository,
                            ConfigRepository configRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.configRepository = configRepository;
    }

    public void loadSampleData() {
        List<User> users = new ArrayList<>();
        users.add(new User("user-ta-01", "ta1@bupt.edu.cn", "ta123", Role.TA));
        users.add(new User("user-ta-02", "ta2@bupt.edu.cn", "ta123", Role.TA));
        users.add(new User("user-mo-01", "mo1@bupt.edu.cn", "mo123", Role.MO));
        users.add(new User("user-admin-01", "admin@bupt.edu.cn", "admin123", Role.ADMIN));
        userRepository.saveAll(users);

        List<ApplicantProfile> profiles = new ArrayList<>();
        ApplicantProfile li = new ApplicantProfile("applicant-01", "user-ta-01");
        li.setName("Li Hua");
        li.setEmail("ta1@bupt.edu.cn");
        li.setPhone("13800000001");
        li.setSkills(List.of("Java", "Communication", "Agile", "Tutoring"));
        li.setAvailability("Mon/Wed afternoon");
        li.setExperienceSummary("Supported Java lab sessions and peer tutoring.");
        li.setPreferredDuties("Lab support, marking");
        li.setCvPath("sample-cv/li-hua-cv.pdf");
        profiles.add(li);

        ApplicantProfile zhang = new ApplicantProfile("applicant-02", "user-ta-02");
        zhang.setName("Zhang Wei");
        zhang.setEmail("ta2@bupt.edu.cn");
        zhang.setPhone("13800000002");
        zhang.setSkills(List.of("Python", "Data Analysis", "Communication"));
        zhang.setAvailability("Tue/Thu full day");
        zhang.setExperienceSummary("Assisted with data science tutorials.");
        zhang.setPreferredDuties("Tutorial support");
        zhang.setCvPath("sample-cv/zhang-wei-cv.pdf");
        profiles.add(zhang);
        profileRepository.saveAll(profiles);

        List<JobPosting> jobs = new ArrayList<>();
        JobPosting javaJob = new JobPosting();
        javaJob.setJobId("job-01");
        javaJob.setModuleCode("COMP1001");
        javaJob.setModuleTitle("Programming Fundamentals");
        javaJob.setDuties("Support labs, answer questions, mark exercises");
        javaJob.setHours(6);
        javaJob.setRequiredSkills(List.of("Java", "Communication", "Agile"));
        javaJob.setApplicationDeadline(LocalDate.now().plusDays(21));
        javaJob.setStatus(JobStatus.CLOSED);
        javaJob.setPostedBy("user-mo-01");
        jobs.add(javaJob);

        JobPosting dataJob = new JobPosting();
        dataJob.setJobId("job-02");
        dataJob.setModuleCode("DATA2002");
        dataJob.setModuleTitle("Data Analytics");
        dataJob.setDuties("Prepare tutorial materials and support datasets");
        dataJob.setHours(8);
        dataJob.setRequiredSkills(List.of("Python", "Data Analysis", "Communication"));
        dataJob.setApplicationDeadline(LocalDate.now().plusDays(14));
        dataJob.setStatus(JobStatus.OPEN);
        dataJob.setPostedBy("user-mo-01");
        jobs.add(dataJob);
        jobRepository.saveAll(jobs);

        List<ApplicationRecord> applications = new ArrayList<>();
        ApplicationRecord record = new ApplicationRecord();
        record.setApplicationId("apply-01");
        record.setApplicantId("applicant-01");
        record.setJobId("job-01");
        record.setAppliedAt(LocalDateTime.now().minusDays(2));
        record.setStatus(ApplicationStatus.ACCEPTED);
        record.setReviewerNotes("Strong fit for lab support.");
        record.setMatchScore(100);
        applications.add(record);
        applicationRepository.saveAll(applications);

        SystemConfig config = new SystemConfig();
        config.setWorkloadThreshold(10);
        configRepository.save(config);
    }

    public void resetData() {
        userRepository.saveAll(new ArrayList<>());
        profileRepository.saveAll(new ArrayList<>());
        jobRepository.saveAll(new ArrayList<>());
        applicationRepository.saveAll(new ArrayList<>());
        configRepository.save(new SystemConfig());
    }
}
