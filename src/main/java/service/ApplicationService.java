package service;

import model.*;
import repository.ApplicationRepository;
import repository.JobRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles application submission and review workflow.
 */
public class ApplicationService {
    private static final Pattern APPLICATION_ID_PATTERN = Pattern.compile("^apply-(\\d+)$");

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final MatchingService matchingService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              MatchingService matchingService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.matchingService = matchingService;
    }

    public ApplicationRecord apply(ApplicantProfile applicantProfile, JobPosting jobPosting) {
        validateApplication(applicantProfile, jobPosting);
        SkillMatchResult matchResult = matchingService.calculateMatch(
                applicantProfile.getSkills(),
                jobPosting.getRequiredSkills()
        );

        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        ApplicationRecord record = new ApplicationRecord();
        record.setApplicationId(nextApplicationId(applications));
        record.setApplicantId(applicantProfile.getApplicantId());
        record.setJobId(jobPosting.getJobId());
        record.setAppliedAt(LocalDateTime.now());
        record.setStatus(ApplicationStatus.SUBMITTED);
        record.setMatchScore(matchResult.getScorePercentage());
        record.setMissingSkills(matchResult.getMissingSkills());
        applications.add(record);
        applicationRepository.saveAll(applications);
        return record;
    }

    public List<ApplicationRecord> getApplicationsForApplicant(String applicantId) {
        return applicationRepository.findByApplicantId(applicantId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getAppliedAt).reversed())
                .toList();
    }

    public List<ApplicationRecord> getApplicationsForJob(String jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getMatchScore).reversed())
                .toList();
    }

    public void updateStatus(String applicationId, ApplicationStatus status, String reviewerNotes) {
        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        ApplicationRecord record = applications.stream()
                .filter(item -> item.getApplicationId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (record.getStatus() == ApplicationStatus.REJECTED || record.getStatus() == ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("Finalized applications cannot be changed.");
        }

        record.setStatus(status);
        record.setReviewerNotes(reviewerNotes);
        applicationRepository.saveAll(applications);

        if (status == ApplicationStatus.ACCEPTED) {
            JobPosting job = jobRepository.findById(record.getJobId()).orElse(null);
            if (job != null) {
                job.setStatus(JobStatus.CLOSED);
                List<JobPosting> jobs = new ArrayList<>(jobRepository.findAll());
                for (int i = 0; i < jobs.size(); i++) {
                    if (jobs.get(i).getJobId().equals(job.getJobId())) {
                        jobs.set(i, job);
                        break;
                    }
                }
                jobRepository.saveAll(jobs);
            }
        }
    }

    private void validateApplication(ApplicantProfile applicantProfile, JobPosting jobPosting) {
        if (jobPosting.getStatus() != JobStatus.OPEN) {
            throw new IllegalStateException("This job is closed.");
        }
        if (jobPosting.getApplicationDeadline() != null && jobPosting.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new IllegalStateException("The application deadline has passed.");
        }
        if (applicantProfile.getCvPath() == null || applicantProfile.getCvPath().isBlank()) {
            throw new IllegalStateException("Please provide a CV path before applying.");
        }
        boolean duplicate = applicationRepository.findByApplicantId(applicantProfile.getApplicantId()).stream()
                .anyMatch(existing -> existing.getJobId().equals(jobPosting.getJobId()));
        if (duplicate) {
            throw new IllegalStateException("Duplicate applications are not allowed.");
        }
    }

    private String nextApplicationId(List<ApplicationRecord> applications) {
        int nextSequence = applications.stream()
                .map(ApplicationRecord::getApplicationId)
                .mapToInt(this::extractApplicationSequence)
                .max()
                .orElse(0) + 1;
        return String.format("apply-%02d", nextSequence);
    }

    private int extractApplicationSequence(String applicationId) {
        if (applicationId == null) {
            return 0;
        }
        Matcher matcher = APPLICATION_ID_PATTERN.matcher(applicationId.trim());
        if (!matcher.matches()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }
}
