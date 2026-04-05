package service;

import model.*;
import repository.ApplicationRepository;
import repository.JobRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        ApplicationRecord record = findRejectedApplication(applications, applicantProfile.getApplicantId(), jobPosting.getJobId())
                .orElseGet(ApplicationRecord::new);
        if (record.getApplicationId() == null || record.getApplicationId().isBlank()) {
            record.setApplicationId(nextApplicationId(applications));
            record.setApplicantId(applicantProfile.getApplicantId());
            record.setJobId(jobPosting.getJobId());
            applications.add(record);
        }
        record.setAppliedAt(LocalDateTime.now());
        record.setStatus(ApplicationStatus.SUBMITTED);
        record.setMatchScore(matchResult.getScorePercentage());
        record.setMissingSkills(matchResult.getMissingSkills());
        record.setReviewerNotes(null);
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

        if (status == ApplicationStatus.ACCEPTED) {
            clearOtherAcceptedApplications(record, applications);
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

        applicationRepository.saveAll(applications);
    }

    public void reopenJob(String jobId) {
        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        boolean updated = false;
        for (ApplicationRecord application : applications) {
            if (jobId.equals(application.getJobId()) && application.getStatus() == ApplicationStatus.ACCEPTED) {
                application.setStatus(ApplicationStatus.SHORTLISTED);
                application.setReviewerNotes(appendNote(
                        application.getReviewerNotes(),
                        "Acceptance cleared because the job was reopened."
                ));
                updated = true;
            }
        }
        if (updated) {
            applicationRepository.saveAll(applications);
        }
    }

    public void cancelAcceptance(String applicationId, String reviewerNotes) {
        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        ApplicationRecord record = applications.stream()
                .filter(item -> item.getApplicationId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (record.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted applications can be cancelled.");
        }

        record.setStatus(ApplicationStatus.SHORTLISTED);
        record.setReviewerNotes(appendNote(
                reviewerNotes,
                "Acceptance cancelled and the job was reopened."
        ));

        JobPosting job = jobRepository.findById(record.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));
        job.setStatus(JobStatus.OPEN);

        List<JobPosting> jobs = new ArrayList<>(jobRepository.findAll());
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getJobId().equals(job.getJobId())) {
                jobs.set(i, job);
                break;
            }
        }

        applicationRepository.saveAll(applications);
        jobRepository.saveAll(jobs);
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
                .anyMatch(existing -> existing.getJobId().equals(jobPosting.getJobId())
                        && existing.getStatus() != ApplicationStatus.REJECTED);
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

    private Optional<ApplicationRecord> findRejectedApplication(List<ApplicationRecord> applications,
                                                                String applicantId,
                                                                String jobId) {
        return applications.stream()
                .filter(existing -> applicantId.equals(existing.getApplicantId())
                        && jobId.equals(existing.getJobId())
                        && existing.getStatus() == ApplicationStatus.REJECTED)
                .findFirst();
    }

    private void clearOtherAcceptedApplications(ApplicationRecord selectedRecord, List<ApplicationRecord> applications) {
        for (ApplicationRecord application : applications) {
            if (!application.getApplicationId().equals(selectedRecord.getApplicationId())
                    && application.getJobId().equals(selectedRecord.getJobId())
                    && application.getStatus() == ApplicationStatus.ACCEPTED) {
                application.setStatus(ApplicationStatus.SHORTLISTED);
                application.setReviewerNotes(appendNote(
                        application.getReviewerNotes(),
                        "Acceptance cleared because another applicant was accepted."
                ));
            }
        }
    }

    private String appendNote(String existingNotes, String note) {
        if (existingNotes == null || existingNotes.isBlank()) {
            return note;
        }
        if (existingNotes.contains(note)) {
            return existingNotes;
        }
        return existingNotes + "\n" + note;
    }
}
