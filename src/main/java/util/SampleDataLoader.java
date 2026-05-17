package util;

import model.*;
import repository.*;
import service.MatchingService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads deterministic local-only demo data for viva use.
 */
public class SampleDataLoader {
    private static final String DEMO_PASSWORD = "Password123";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final AllocationRepository allocationRepository;
    private final ConfigRepository configRepository;
    private final MatchingService matchingService = new MatchingService();

    public SampleDataLoader(UserRepository userRepository,
                            ApplicantProfileRepository profileRepository,
                            JobRepository jobRepository,
                            ApplicationRepository applicationRepository,
                            NotificationRepository notificationRepository,
                            MessageRepository messageRepository,
                            AllocationRepository allocationRepository,
                            ConfigRepository configRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.notificationRepository = notificationRepository;
        this.messageRepository = messageRepository;
        this.allocationRepository = allocationRepository;
        this.configRepository = configRepository;
    }

    public void loadSampleData() {
        List<NotificationRecord> existingNotifications = notificationRepository.findAll();
        List<MessageRecord> existingMessages = messageRepository.findAll();
        List<JobPosting> existingJobs = jobRepository.findAll();

        List<User> users = buildUsers();
        userRepository.saveAll(users);

        List<ApplicantProfile> profiles = buildProfiles();
        profileRepository.saveAll(profiles);

        List<JobPosting> jobs = mergeJobsPreservingExistingEdits(buildJobs(), existingJobs);
        jobRepository.saveAll(jobs);

        List<ApplicationRecord> applications = buildApplications(profiles, jobs);
        applicationRepository.saveAll(applications);

        allocationRepository.saveAll(buildAllocations(applications, jobs));
        notificationRepository.saveAll(preserveNotificationReadState(buildNotifications(), existingNotifications));
        messageRepository.saveAll(preserveMessageReadState(buildMessages(), existingMessages));

        SystemConfig config = new SystemConfig();
        config.setWorkloadThreshold(10);
        configRepository.save(config);
    }

    private List<JobPosting> mergeJobsPreservingExistingEdits(List<JobPosting> demoJobs, List<JobPosting> existingJobs) {
        Map<String, JobPosting> mergedJobs = new LinkedHashMap<>();
        for (JobPosting demoJob : demoJobs) {
            mergedJobs.put(demoJob.getJobId(), demoJob);
        }
        for (JobPosting existingJob : existingJobs) {
            if (existingJob.getJobId() != null && !existingJob.getJobId().isBlank()) {
                mergedJobs.put(existingJob.getJobId(), existingJob);
            }
        }
        return new ArrayList<>(mergedJobs.values());
    }

    private List<NotificationRecord> preserveNotificationReadState(List<NotificationRecord> freshNotifications,
                                                                   List<NotificationRecord> existingNotifications) {
        for (NotificationRecord fresh : freshNotifications) {
            existingNotifications.stream()
                    .filter(existing -> sameNotification(fresh, existing))
                    .findFirst()
                    .ifPresent(existing -> fresh.setRead(existing.isRead()));
        }
        return freshNotifications;
    }

    private boolean sameNotification(NotificationRecord left, NotificationRecord right) {
        return left.getNotificationId().equals(right.getNotificationId())
                && left.getUserId().equals(right.getUserId())
                && left.getMessage().equals(right.getMessage());
    }

    private List<MessageRecord> preserveMessageReadState(List<MessageRecord> freshMessages,
                                                         List<MessageRecord> existingMessages) {
        for (MessageRecord fresh : freshMessages) {
            existingMessages.stream()
                    .filter(existing -> sameMessage(fresh, existing))
                    .findFirst()
                    .ifPresent(existing -> fresh.setRead(existing.isRead()));
        }
        return freshMessages;
    }

    private boolean sameMessage(MessageRecord left, MessageRecord right) {
        return left.getMessageId().equals(right.getMessageId())
                && left.getSenderUserId().equals(right.getSenderUserId())
                && left.getRecipientUserId().equals(right.getRecipientUserId())
                && left.getBody().equals(right.getBody());
    }

    public void resetData() {
        userRepository.saveAll(new ArrayList<>());
        profileRepository.saveAll(new ArrayList<>());
        jobRepository.saveAll(new ArrayList<>());
        applicationRepository.saveAll(new ArrayList<>());
        notificationRepository.saveAll(new ArrayList<>());
        messageRepository.saveAll(new ArrayList<>());
        allocationRepository.saveAll(new ArrayList<>());
        configRepository.save(new SystemConfig());
    }

    private List<User> buildUsers() {
        return List.of(
                user("user-admin-01", "System Admin", "admin@bupt.edu.cn", ADMIN_PASSWORD, Role.ADMIN, List.of()),
                user("user-mo-ling", "Dr Ling Ma", "ling.ma@qmul.ac.uk", DEMO_PASSWORD, Role.MO, List.of("EBU6304")),
                user("user-mo-chao-shu", "Dr Chao Shu", "chao.shu@qmul.ac.uk", DEMO_PASSWORD, Role.MO, List.of("EBU6475")),
                user("user-mo-jin-zhang", "Dr Jin Zhang", "jin.zhang@qmul.ac.uk", DEMO_PASSWORD, Role.MO, List.of("EBU6366")),
                user("user-mo-nickos", "Dr Nickos Paltalidis", "n.paltalidis@qmul.ac.uk", DEMO_PASSWORD, Role.MO, List.of("EBU5606")),
                user("user-mo-paula", "Dr Paula Fonseca", "paula.fonseca@qmul.ac.uk", DEMO_PASSWORD, Role.MO, List.of("EBU5042")),
                user("user-mo-chao-liu", "Dr Chao Liu", "chao.liu@qmul.ac.uk", DEMO_PASSWORD, Role.MO, List.of("CBU5201")),
                user("user-mo-athen", "Dr Athen Ma", "a.ma@qmul.ac.uk", DEMO_PASSWORD, Role.MO, List.of("EBU6335")),
                user("user-ta-alice", "Alice Chen", "alice.chen@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-ben", "Ben Wang", "ben.wang@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-chloe", "Chloe Li", "chloe.li@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-daniel", "Daniel Zhang", "daniel.zhang@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-emma", "Emma Liu", "emma.liu@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-frank", "Frank Zhao", "frank.zhao@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-grace", "Grace Xu", "grace.xu@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-henry", "Henry Sun", "henry.sun@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-ivy", "Ivy Huang", "ivy.huang@demo.local", DEMO_PASSWORD, Role.TA, List.of()),
                user("user-ta-jason", "Jason Wu", "jason.wu@demo.local", DEMO_PASSWORD, Role.TA, List.of())
        );
    }

    private List<ApplicantProfile> buildProfiles() {
        return List.of(
                profile("applicant-alice", "user-ta-alice", "Alice Chen", "alice.chen@demo.local", "13800000011",
                        "Software Engineering", "Year 3",
                        List.of("Java", "Software Engineering", "Agile Development", "GitHub", "Testing"),
                        "8 hours/week", "I have strong Java programming experience and have completed several coursework projects using GitHub and Agile development practices. I am confident in supporting EBU6304 labs and helping students debug basic software engineering issues.",
                        "EBU6304 lab support", "cv/alice-chen-cv.docx", List.of("job-ebu6304")),
                profile("applicant-ben", "user-ta-ben", "Ben Wang", "ben.wang@demo.local", "13800000012",
                        "Electronic Engineering", "Year 3",
                        List.of("C Programming", "Microcontrollers", "Embedded Systems", "Digital Systems", "Debugging"),
                        "7 hours/week", "I am familiar with C programming and embedded system debugging. I have experience with microcontroller labs and can help students understand hardware/software interaction.",
                        "EBU6475 lab support", "cv/ben-wang-cv.docx", List.of("job-ebu6475")),
                profile("applicant-chloe", "user-ta-chloe", "Chloe Li", "chloe.li@demo.local", "13800000013",
                        "Telecommunications Engineering", "Year 4",
                        List.of("MATLAB", "Signal Analysis", "RF Systems", "Microwave Engineering"),
                        "6 hours/week", "I have a good background in communication systems and MATLAB-based signal analysis. I can support students in understanding RF and microwave-related exercises.",
                        "EBU6366 tutorials", "cv/chloe-li-cv.docx", List.of("job-ebu6366")),
                profile("applicant-daniel", "user-ta-daniel", "Daniel Zhang", "daniel.zhang@demo.local", "13800000014",
                        "Business and Engineering", "Year 3",
                        List.of("Product Development", "Marketing", "Business Analysis", "Report Writing", "Presentation"),
                        "5 hours/week", "I have experience preparing business reports and product concept evaluations. I can support students with customer needs analysis, report structure and presentation preparation.",
                        "EBU5606 workshops", "cv/daniel-zhang-cv.docx", List.of("job-ebu5606")),
                profile("applicant-emma", "user-ta-emma", "Emma Liu", "emma.liu@demo.local", "13800000015",
                        "Data Science", "Year 3",
                        List.of("Python", "Machine Learning", "Data Analysis", "Mathematics"),
                        "6 hours/week", "I am strong in Python and data analysis, but I have limited experience in Java and Agile project management. I am willing to learn missing skills before supporting software engineering modules.",
                        "EBU6304 after preparation", "cv/emma-liu-cv.docx", List.of("job-ebu6304")),
                profile("applicant-frank", "user-ta-frank", "Frank Zhao", "frank.zhao@demo.local", "13800000016",
                        "Computer Engineering", "Year 4",
                        List.of("Java", "C Programming", "MATLAB", "Debugging", "Presentation"),
                        "10 hours/week", "I have a mixed technical background and can support programming-related lab sessions. I am available for multiple modules but my workload should be monitored carefully.",
                        "Programming and lab support", "cv/frank-zhao-cv.docx", List.of("job-ebu6304", "job-ebu6475", "job-ebu6366")),
                profile("applicant-grace", "user-ta-grace", "Grace Xu", "grace.xu@demo.local", "13800000017",
                        "Computer Science", "Year 3",
                        List.of("Java", "Network Programming", "Socket Programming", "Client Server Architecture", "Debugging"),
                        "6 hours/week", "I have experience with Java socket programming and client-server coursework. I can help students debug network applications and explain basic communication protocols.",
                        "EBU5042 lab support", "cv/grace-xu-cv.docx", List.of("job-ebu5042")),
                profile("applicant-henry", "user-ta-henry", "Henry Sun", "henry.sun@demo.local", "13800000018",
                        "Artificial Intelligence", "Year 3",
                        List.of("Python", "Machine Learning", "Data Analysis", "Mathematics", "Jupyter Notebook"),
                        "7 hours/week", "I am confident in Python, Jupyter Notebook and basic machine learning models. I can support students with data preprocessing, model training and coursework notebooks.",
                        "Machine Learning tutorials", "cv/henry-sun-cv.docx", List.of("job-cbu5201")),
                profile("applicant-ivy", "user-ta-ivy", "Ivy Huang", "ivy.huang@demo.local", "13800000019",
                        "Electronic Engineering", "Year 3",
                        List.of("Digital Logic", "Verilog", "FPGA", "Circuit Design", "Debugging"),
                        "6 hours/week", "I have completed digital systems and FPGA-related projects. I can help students with Verilog syntax, waveform debugging and digital logic design.",
                        "EBU6335 lab support", "cv/ivy-huang-cv.docx", List.of("job-ebu6335")),
                profile("applicant-jason", "user-ta-jason", "Jason Wu", "jason.wu@demo.local", "13800000020",
                        "Engineering Management", "Year 4",
                        List.of("Java", "Python", "MATLAB", "Report Writing", "Presentation"),
                        "9 hours/week", "I have a broad technical background and can support programming, data analysis and report preparation. However, I may need additional preparation for specialised RF or FPGA topics.",
                        "Programming and report support", "cv/jason-wu-cv.docx", List.of("job-ebu6304", "job-cbu5201", "job-ebu5606"))
        );
    }

    private List<JobPosting> buildJobs() {
        return List.of(
                job("job-ebu6304", "EBU6304", "Software Engineering - 2025/26",
                        "Support software engineering labs, answer student questions, assist with Java project issues, help review Agile artefacts, and support coursework demonstrations. Module Organiser: Dr Ling Ma. Teaching team: Dr Gokop Goteng; Dr Riasat Islam; Dr Salman Haleem; Dr Alan Wong.",
                        6, 3, List.of("Java", "Software Engineering", "Agile Development", "GitHub", "Testing"),
                        LocalDate.now().plusDays(28), "user-mo-ling",
                        "Friday 14:00-16:00 lab support; flexible coursework support", "Software Engineering Lab"),
                job("job-ebu6475", "EBU6475", "Microprocessor Systems Design - 2025/26",
                        "Support embedded systems labs, help students with microcontroller programming, assist debugging hardware/software issues, and support lab demonstrations. Module Organiser: Dr Chao Shu.",
                        6, 2, List.of("C Programming", "Microcontrollers", "Embedded Systems", "Digital Systems", "Debugging"),
                        LocalDate.now().plusDays(26), "user-mo-chao-shu",
                        "Tuesday 10:00-12:00 lab support", "Embedded Systems Lab"),
                job("job-ebu6366", "EBU6366", "Microwave, Millimeterwave and Optical Transmission - 2025/26",
                        "Support tutorials and lab preparation for microwave, millimeterwave and optical transmission topics, assist students with RF concepts, MATLAB exercises, and coursework questions. Related staff: Dr Fatma Benkhelifa.",
                        5, 2, List.of("Microwave Engineering", "RF Systems", "Optical Transmission", "MATLAB", "Signal Analysis"),
                        LocalDate.now().plusDays(24), "user-mo-jin-zhang",
                        "Wednesday 15:00-17:00 tutorial support", "RF and Microwave Lab"),
                job("job-ebu5606", "EBU5606", "Product Development and Marketing - 2025/26",
                        "Support product development workshops, help students refine business reports and presentation materials, assist with customer needs analysis, concept testing, and marketing coursework activities.",
                        4, 2, List.of("Product Development", "Marketing", "Business Analysis", "Report Writing", "Presentation"),
                        LocalDate.now().plusDays(30), "user-mo-nickos",
                        "Monday 13:00-15:00 workshop support", "Business Innovation Classroom"),
                job("job-ebu5042", "EBU5042", "Advanced Network Programming - 2025/26",
                        "Support advanced network programming labs, help students debug socket-based applications, answer questions about client-server architecture, and assist with coursework support. Related staff: Prof Gareth Tyson.",
                        5, 2, List.of("Java", "Network Programming", "Socket Programming", "Client Server Architecture", "Debugging"),
                        LocalDate.now().plusDays(25), "user-mo-paula",
                        "Thursday 10:00-12:00 programming lab", "Network Programming Lab"),
                job("job-cbu5201", "CBU5201", "Machine Learning - 2025/26",
                        "Support machine learning tutorials, help students understand basic models, assist with Python notebooks, and answer questions about coursework tasks.",
                        5, 2, List.of("Python", "Machine Learning", "Data Analysis", "Mathematics", "Jupyter Notebook"),
                        LocalDate.now().plusDays(29), "user-mo-chao-liu",
                        "Tuesday 15:00-17:00 tutorial support", "AI Teaching Lab"),
                job("job-ebu6335", "EBU6335", "Digital Systems Design - 2025/26",
                        "Support digital systems design labs, help students understand digital logic, Verilog and FPGA workflows, and assist with debugging design exercises.",
                        5, 2, List.of("Digital Logic", "Verilog", "FPGA", "Circuit Design", "Debugging"),
                        LocalDate.now().plusDays(27), "user-mo-athen",
                        "Friday 10:00-12:00 digital systems lab", "Digital Systems Lab"),
                oneOffJob("job-ebu6304-invigilation", "EBU6304", "Invigilation Assistant for EBU6304 Final Assessment",
                        "Assist with exam room preparation, student check-in, invigilation support, and post-exam material collection.",
                        3, List.of("Responsibility", "Communication", "Time Management", "Exam Procedure Awareness"),
                        LocalDate.parse("2026-05-23"), "user-mo-ling", JobPosting.JOB_TYPE_INVIGILATION,
                        "2026-05-24", "2026-05-24", "09:00-12:00", "Teaching Building Room 302", JobCategory.INVIGILATION),
                oneOffJob("job-ebu6304-demo", "EBU6304", "Demo Support Assistant for EBU6304 Final Assessment",
                        "Support final coursework demonstration sessions, help organise demo order, assist with technical checks, and support marking logistics.",
                        4, List.of("Software Engineering", "Communication", "Java", "Testing"),
                        LocalDate.parse("2026-05-23"), "user-mo-ling", JobPosting.JOB_TYPE_DEMO_SUPPORT,
                        "2026-05-24", "2026-05-24", "13:00-17:00", "Software Engineering Lab", JobCategory.OTHER_ACTIVITY)
        );
    }

    private List<ApplicationRecord> buildApplications(List<ApplicantProfile> profiles, List<JobPosting> jobs) {
        return List.of(
                application("apply-01", "applicant-alice", "job-ebu6304", ApplicationStatus.SHORTLISTED,
                        "Strong match for software engineering labs. Good Java and GitHub background. Recommended for shortlist.",
                        LocalDateTime.now().minusDays(6), "user-mo-ling", profiles, jobs),
                application("apply-02", "applicant-ben", "job-ebu6475", ApplicationStatus.INTERVIEW_INVITED,
                        "Good embedded systems background and practical debugging skills. Suitable for microprocessor lab support.",
                        LocalDateTime.now().minusDays(5), "user-mo-chao-shu", profiles, jobs),
                application("apply-03", "applicant-chloe", "job-ebu6366", ApplicationStatus.SHORTLISTED,
                        "Relevant RF and MATLAB skills. Needs to review optical transmission topics before assisting tutorials.",
                        LocalDateTime.now().minusDays(4), "user-mo-jin-zhang", profiles, jobs),
                application("apply-04", "applicant-daniel", "job-ebu5606", ApplicationStatus.SUBMITTED,
                        "Good written communication and product analysis skills. Suitable for supporting report-based workshops.",
                        LocalDateTime.now().minusDays(3), "user-mo-nickos", profiles, jobs),
                application("apply-05", "applicant-emma", "job-ebu6304", ApplicationStatus.REJECTED,
                        "Strong analytical background but missing Java, GitHub, Testing and Agile Development experience. Not currently suitable for EBU6304 without preparation.",
                        LocalDateTime.now().minusDays(2), "user-mo-ling", profiles, jobs),
                application("apply-06", "applicant-frank", "job-ebu6475", ApplicationStatus.ACCEPTED,
                        "Flexible technical background and useful C programming experience. Accepted, but workload should be monitored.",
                        LocalDateTime.now().minusDays(2), "user-mo-chao-shu", profiles, jobs),
                application("apply-07", "applicant-frank", "job-ebu6366", ApplicationStatus.ACCEPTED,
                        "Has MATLAB and debugging background, and is accepted for tutorial support. Workload should be monitored because this is an additional TA role.",
                        LocalDateTime.now().minusHours(18), "user-mo-jin-zhang", profiles, jobs),
                application("apply-08", "applicant-grace", "job-ebu5042", ApplicationStatus.SHORTLISTED,
                        "Strong match for Java network programming labs. Good socket programming and debugging background.",
                        LocalDateTime.now().minusDays(4), "user-mo-paula", profiles, jobs),
                application("apply-09", "applicant-henry", "job-cbu5201", ApplicationStatus.INTERVIEW_INVITED,
                        "Strong Python and machine learning background. Suitable for supporting notebooks and model explanation.",
                        LocalDateTime.now().minusDays(3), "user-mo-chao-liu", profiles, jobs),
                application("apply-10", "applicant-ivy", "job-ebu6335", ApplicationStatus.SHORTLISTED,
                        "Strong digital logic and Verilog background. Recommended for FPGA lab support.",
                        LocalDateTime.now().minusDays(3), "user-mo-athen", profiles, jobs),
                application("apply-11", "applicant-jason", "job-ebu5606", ApplicationStatus.ACCEPTED,
                        "Good communication and presentation skills. Suitable for workshop and report support.",
                        LocalDateTime.now().minusDays(2), "user-mo-nickos", profiles, jobs),
                application("apply-12", "applicant-jason", "job-ebu6366", ApplicationStatus.SUBMITTED,
                        "General MATLAB experience is useful, but missing specialised microwave and RF systems knowledge.",
                        LocalDateTime.now().minusDays(1), "user-mo-jin-zhang", profiles, jobs),
                application("apply-13", "applicant-emma", "job-cbu5201", ApplicationStatus.SHORTLISTED,
                        "Strong fit for machine learning tutorials. Good Python, mathematics and data analysis background.",
                        LocalDateTime.now().minusHours(20), "user-mo-chao-liu", profiles, jobs),
                application("apply-14", "applicant-alice", "job-ebu6304-invigilation", ApplicationStatus.ACCEPTED,
                        "Reliable candidate with strong communication skills. Accepted for the final assessment invigilation event.",
                        LocalDateTime.now().minusHours(12), "user-mo-ling", profiles, jobs),
                application("apply-15", "applicant-jason", "job-ebu6304-demo", ApplicationStatus.ACCEPTED,
                        "Broad technical background and presentation support experience. Accepted for final demo logistics and technical checks.",
                        LocalDateTime.now().minusHours(10), "user-mo-ling", profiles, jobs)
        );
    }

    private List<AllocationRecord> buildAllocations(List<ApplicationRecord> applications, List<JobPosting> jobs) {
        List<AllocationRecord> allocations = new ArrayList<>();
        int sequence = 1;
        for (ApplicationRecord application : applications) {
            if (application.getStatus() != ApplicationStatus.ACCEPTED) {
                continue;
            }
            JobPosting job = jobs.stream()
                    .filter(item -> application.getJobId().equals(item.getJobId()))
                    .findFirst()
                    .orElse(null);
            AllocationRecord allocation = new AllocationRecord();
            allocation.setAllocationId(String.format("alloc-%02d", sequence++));
            allocation.setApplicationId(application.getApplicationId());
            allocation.setApplicantId(application.getApplicantId());
            allocation.setJobId(application.getJobId());
            allocation.setAllocatedByUserId(job == null ? "" : job.getPostedBy());
            allocation.setAllocatedAt(LocalDateTime.now().minusDays(1));
            allocation.setActive(true);
            allocations.add(allocation);
        }
        return allocations;
    }

    private List<NotificationRecord> buildNotifications() {
        return List.of(
                notification("note-01", "user-ta-alice", "Your application for EBU6304 has been shortlisted.", LocalDateTime.now().minusHours(20)),
                notification("note-02", "user-ta-ben", "You have been invited to interview for EBU6475.", LocalDateTime.now().minusHours(18)),
                notification("note-03", "user-ta-chloe", "Your application for EBU6366 has been shortlisted.", LocalDateTime.now().minusHours(16)),
                notification("note-04", "user-ta-frank", "Your accepted TA workload now exceeds the recommended 10 hours/week threshold.", LocalDateTime.now().minusHours(12)),
                notification("note-05", "user-ta-emma", "Your EBU6304 application has missing required skills.", LocalDateTime.now().minusHours(10)),
                notification("note-06", "user-ta-grace", "Your application for EBU5042 has been shortlisted.", LocalDateTime.now().minusHours(9)),
                notification("note-07", "user-ta-henry", "You have been invited to interview for Machine Learning.", LocalDateTime.now().minusHours(8)),
                notification("note-08", "user-ta-ivy", "Your application for EBU6335 has been shortlisted.", LocalDateTime.now().minusHours(7)),
                notification("note-09", "user-ta-jason", "Your EBU6366 application has missing specialised RF skills.", LocalDateTime.now().minusHours(6)),
                notification("note-10", "user-ta-emma", "Your Machine Learning application is a strong match.", LocalDateTime.now().minusHours(5))
        );
    }

    private List<MessageRecord> buildMessages() {
        return List.of(
                message("msg-01", "job-ebu6304", "apply-01", "user-mo-ling", "user-ta-alice",
                        "Please prepare to discuss your Java project and GitHub experience during the interview.",
                        LocalDateTime.now().minusHours(22)),
                message("msg-02", "job-ebu6475", "apply-02", "user-mo-chao-shu", "user-ta-ben",
                        "Please bring examples of your embedded systems lab experience.",
                        LocalDateTime.now().minusHours(19)),
                message("msg-03", "job-ebu6366", "apply-03", "user-mo-jin-zhang", "user-ta-chloe",
                        "Please review microwave transmission line basics before the tutorial support session.",
                        LocalDateTime.now().minusHours(17)),
                message("msg-04", "job-ebu5606", "apply-04", "user-mo-nickos", "user-ta-daniel",
                        "Please prepare examples of product analysis or business report writing.",
                        LocalDateTime.now().minusHours(14)),
                message("msg-05", "job-ebu5042", "apply-08", "user-mo-paula", "user-ta-grace",
                        "Please prepare to discuss your Java socket programming and client-server coursework experience.",
                        LocalDateTime.now().minusHours(10)),
                message("msg-06", "job-cbu5201", "apply-09", "user-mo-chao-liu", "user-ta-henry",
                        "Please prepare examples of Python notebooks or machine learning coursework you have completed.",
                        LocalDateTime.now().minusHours(9)),
                message("msg-07", "job-ebu6335", "apply-10", "user-mo-athen", "user-ta-ivy",
                        "Please prepare to explain your Verilog and FPGA debugging experience.",
                        LocalDateTime.now().minusHours(8)),
                message("msg-08", "job-ebu5606", "apply-11", "user-mo-nickos", "user-ta-jason",
                        "Please prepare examples of report writing or presentation support experience.",
                        LocalDateTime.now().minusHours(7))
        );
    }

    private User user(String userId, String name, String username, String password, Role role, List<String> modules) {
        return new User(userId, name, username, PasswordUtil.hash(password), role, modules);
    }

    private ApplicantProfile profile(String applicantId,
                                     String userId,
                                     String name,
                                     String email,
                                     String phone,
                                     String programme,
                                     String year,
                                     List<String> skills,
                                     String availability,
                                     String selfEvaluation,
                                     String preferredDuties,
                                     String cvPath,
                                     List<String> favouriteJobs) {
        ApplicantProfile profile = new ApplicantProfile(applicantId, userId);
        profile.setName(name);
        profile.setEmail(email);
        profile.setPhone(phone);
        profile.setProgramme(programme);
        profile.setYearOfStudy(year);
        profile.setSkills(skills);
        profile.setAvailability(availability);
        profile.setExperienceSummary(selfEvaluation
                + "\n\nSupporting documents include: award certificate, competition participation proof, and additional evidence material.");
        profile.setPreferredDuties(preferredDuties);
        profile.setCvPath(cvPath);
        profile.setSupportingDocumentPaths(supportingDocumentsFor(name));
        profile.setFavoriteJobIds(favouriteJobs);
        return profile;
    }

    private List<String> supportingDocumentsFor(String applicantName) {
        String slug = applicantName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return List.of(
                "supporting-documents/" + slug + "-award-certificate.pdf",
                "supporting-documents/" + slug + "-competition-proof.pdf",
                "supporting-documents/" + slug + "-supporting-evidence.pdf"
        );
    }

    private JobPosting job(String jobId,
                           String code,
                           String title,
                           String duties,
                           int hours,
                           int requiredTaCount,
                           List<String> skills,
                           LocalDate deadline,
                           String postedBy,
                           String schedule,
                           String location) {
        JobPosting job = new JobPosting();
        job.setJobId(jobId);
        job.setModuleCode(code);
        job.setModuleTitle(title);
        job.setCategory(JobCategory.MODULE_TA);
        job.setSemester("2025/26");
        job.setDuties(duties);
        job.setHours(hours);
        job.setJobType(JobPosting.JOB_TYPE_COURSE_SUPPORT);
        job.setStartDate("2026-03-06");
        job.setEndDate("2026-05-24");
        job.setSchedule(schedule);
        job.setLocation(location);
        job.setWorkloadType(JobPosting.WORKLOAD_TYPE_WEEKLY);
        job.setRequiredTaCount(requiredTaCount);
        job.setRequiredSkills(skills);
        job.setApplicationDeadline(deadline);
        job.setStatus(JobStatus.OPEN);
        job.setPostedBy(postedBy);
        return job;
    }

    private JobPosting oneOffJob(String jobId,
                                 String code,
                                 String title,
                                 String duties,
                                 int hours,
                                 List<String> skills,
                                 LocalDate deadline,
                                 String postedBy,
                                 String jobType,
                                 String startDate,
                                 String endDate,
                                 String schedule,
                                 String location,
                                 JobCategory category) {
        JobPosting job = new JobPosting();
        job.setJobId(jobId);
        job.setModuleCode(code);
        job.setModuleTitle(title);
        job.setCategory(category);
        job.setSemester("2025/26");
        job.setDuties(duties + " Module Organiser: Dr Ling Ma.");
        job.setHours(hours);
        job.setJobType(jobType);
        job.setStartDate(startDate);
        job.setEndDate(endDate);
        job.setSchedule(schedule);
        job.setLocation(location);
        job.setWorkloadType(JobPosting.WORKLOAD_TYPE_TOTAL);
        job.setRequiredTaCount(2);
        job.setRequiredSkills(skills);
        job.setApplicationDeadline(deadline);
        job.setStatus(JobStatus.OPEN);
        job.setPostedBy(postedBy);
        return job;
    }

    private ApplicationRecord application(String id,
                                          String applicantId,
                                          String jobId,
                                          ApplicationStatus status,
                                          String moEvaluation,
                                          LocalDateTime appliedAt,
                                          String actorUserId,
                                          List<ApplicantProfile> profiles,
                                          List<JobPosting> jobs) {
        ApplicantProfile profile = profiles.stream()
                .filter(item -> applicantId.equals(item.getApplicantId()))
                .findFirst()
                .orElseThrow();
        JobPosting job = jobs.stream()
                .filter(item -> jobId.equals(item.getJobId()))
                .findFirst()
                .orElseThrow();
        SkillMatchResult match = matchingService.calculateMatch(profile.getSkills(), job.getRequiredSkills());

        ApplicationRecord application = new ApplicationRecord();
        application.setApplicationId(id);
        application.setApplicantId(applicantId);
        application.setJobId(jobId);
        application.setAppliedAt(appliedAt);
        application.setLastUpdatedAt(appliedAt.plusHours(8));
        application.setDecisionAt(status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED
                ? appliedAt.plusHours(8)
                : null);
        application.setStatus(status);
        application.setReviewerNotes(moEvaluation);
        application.setMatchScore(match.getScorePercentage());
        application.setMissingSkills(match.getMissingSkills());
        application.setStatusHistory(List.of(
                new StatusHistoryEntry(ApplicationStatus.SUBMITTED, appliedAt, profile.getUserId(), "Application submitted."),
                new StatusHistoryEntry(status, appliedAt.plusHours(8), actorUserId, moEvaluation)
        ));
        return application;
    }

    private NotificationRecord notification(String id, String userId, String message, LocalDateTime createdAt) {
        NotificationRecord notification = new NotificationRecord();
        notification.setNotificationId(id);
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setCreatedAt(createdAt);
        notification.setRead(false);
        return notification;
    }

    private MessageRecord message(String id,
                                  String jobId,
                                  String applicationId,
                                  String senderUserId,
                                  String recipientUserId,
                                  String body,
                                  LocalDateTime createdAt) {
        MessageRecord message = new MessageRecord();
        message.setMessageId(id);
        message.setJobId(jobId);
        message.setApplicationId(applicationId);
        message.setSenderUserId(senderUserId);
        message.setRecipientUserId(recipientUserId);
        message.setBody(body);
        message.setCreatedAt(createdAt);
        message.setRead(false);
        return message;
    }
}
