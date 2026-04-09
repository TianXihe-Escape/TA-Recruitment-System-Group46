package service;

import model.*;
import repository.ApplicationRepository;
import repository.JobRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that manages the application lifecycle from submission through review.
 * It also keeps job status synchronized with accepted offers so hiring progress
 * is reflected consistently across TA and admin views.
 */
public class ApplicationService {
    /**
     * Pattern used to extract the numeric part of generated application ids.
     */
    private static final Pattern APPLICATION_ID_PATTERN = Pattern.compile("^apply-(\\d+)$");

    /**
     * Repository for application persistence.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Repository for job persistence, used when application outcomes affect job status.
     */
    private final JobRepository jobRepository;

    /**
     * Service used to calculate skill-match scores during submission.
     */
    private final MatchingService matchingService;

    /**
     * Creates the application service with its dependencies.
     */
    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              MatchingService matchingService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.matchingService = matchingService;
    }

    /**
     * Submits or resubmits an application for a TA and job pair.
     *
     * @param applicantProfile applicant who is applying
     * @param jobPosting target job posting
     * @return stored application record after submission
     */
    public ApplicationRecord apply(ApplicantProfile applicantProfile, JobPosting jobPosting) {
        validateApplication(applicantProfile, jobPosting);
        SkillMatchResult matchResult = matchingService.calculateMatch(
                applicantProfile.getSkills(),
                jobPosting.getRequiredSkills()
        );

        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        // Reuse a previously rejected record so the same TA/job pair keeps a single audit trail.
        ApplicationRecord record = findRejectedApplication(applications, applicantProfile.getApplicantId(), jobPosting.getJobId())
                .orElseGet(ApplicationRecord::new);
        if (record.getApplicationId() == null || record.getApplicationId().isBlank()) {
            // Only brand-new applications get a generated id and an appended record.
            record.setApplicationId(nextApplicationId(applications));
            record.setApplicantId(applicantProfile.getApplicantId());
            record.setJobId(jobPosting.getJobId());
            applications.add(record);
        }
        // Refresh all submission-state fields so a resubmission behaves like a clean retry.
        record.setAppliedAt(LocalDateTime.now());
        record.setStatus(ApplicationStatus.SUBMITTED);
        record.setMatchScore(matchResult.getScorePercentage());
        record.setMissingSkills(matchResult.getMissingSkills());
        record.setReviewerNotes(null);
        applicationRepository.saveAll(applications);
        return record;
    }

    /**
     * Returns an applicant's applications in reverse chronological order.
     */
    public List<ApplicationRecord> getApplicationsForApplicant(String applicantId) {
        return applicationRepository.findByApplicantId(applicantId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getAppliedAt).reversed())
                .toList();
    }

    /**
     * Returns all applications for a job sorted by best match first.
     */
    public List<ApplicationRecord> getApplicationsForJob(String jobId) {
        return applicationRepository.findByJobId(jobId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getMatchScore).reversed())
                .toList();
    }

    /**
     * Counts all applications tied to a specific job.
     */
    public int getApplicationCountForJob(String jobId) {
        return applicationRepository.findByJobId(jobId).size();
    }

    /**
     * Counts how many applicants for a job are currently accepted.
     */
    public int getAcceptedCountForJob(String jobId) {
        return (int) applicationRepository.findByJobId(jobId).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
    }

    /**
     * Updates review status and reviewer notes for an application.
     * Accepting an application may close the job if the required TA count is met.
     */
    public void updateStatus(String applicationId, ApplicationStatus status, String reviewerNotes) {
        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        ApplicationRecord record = applications.stream()
                .filter(item -> item.getApplicationId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (record.getStatus() == ApplicationStatus.REJECTED
                || record.getStatus() == ApplicationStatus.ACCEPTED
                || record.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("Finalized applications cannot be changed.");
        }

        record.setStatus(status);
        record.setReviewerNotes(reviewerNotes);

        if (status == ApplicationStatus.ACCEPTED) {
            // An accepted offer consumes one slot on the job and may close hiring immediately.
            syncJobStatus(record.getJobId(), applications);
        }

        applicationRepository.saveAll(applications);
    }

    /**
     * Reopens a job by converting accepted applications back into shortlisted ones.
     */
    public void reopenJob(String jobId) {
        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        boolean updated = false;
        for (ApplicationRecord application : applications) {
            if (jobId.equals(application.getJobId()) && application.getStatus() == ApplicationStatus.ACCEPTED) {
                // Reopening puts accepted applicants back into the reviewable pool instead of deleting history.
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
        syncJobStatus(jobId, applications);
    }

    /**
     * Cancels a single accepted application and returns it to the shortlist state.
     */
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

        applicationRepository.saveAll(applications);
        syncJobStatus(record.getJobId(), applications);
    }

    /**
     * Allows applicants to withdraw an active application.
     */
    public void withdrawApplication(String applicationId) {
        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        ApplicationRecord record = applications.stream()
                .filter(item -> item.getApplicationId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (record.getStatus() == ApplicationStatus.ACCEPTED || record.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Finalized applications cannot be withdrawn.");
        }
        if (record.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("This application has already been withdrawn.");
        }

        record.setStatus(ApplicationStatus.WITHDRAWN);
        record.setReviewerNotes(appendNote(record.getReviewerNotes(), "Withdrawn by applicant."));
        applicationRepository.saveAll(applications);
    }

    /**
     * Removes every application belonging to a deleted applicant and then recalculates
     * the status of each affected job.
     */
    public void removeApplicationsForApplicant(String applicantId) {
        List<ApplicationRecord> applications = new ArrayList<>(applicationRepository.findAll());
        LinkedHashSet<String> affectedJobIds = new LinkedHashSet<>();
        for (ApplicationRecord record : applications) {
            if (!applicantId.equals(record.getApplicantId())) {
                continue;
            }
            String jobId = record.getJobId();
            if (jobId != null && !jobId.isBlank()) {
                affectedJobIds.add(jobId);
            }
        }

        if (affectedJobIds.isEmpty()) {
            return;
        }

        applications.removeIf(record -> applicantId.equals(record.getApplicantId()));
        applicationRepository.saveAll(applications);
        for (String jobId : affectedJobIds) {
            syncJobStatus(jobId, applications);
        }
    }

    /**
     * Verifies whether an application can be submitted under the current rules.
     */
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
                        && existing.getStatus() != ApplicationStatus.REJECTED
                        && existing.getStatus() != ApplicationStatus.WITHDRAWN);
        if (duplicate) {
            throw new IllegalStateException("Duplicate applications are not allowed.");
        }
    }

    /**
     * Generates the next application id from the current stored sequence.
     */
    private String nextApplicationId(List<ApplicationRecord> applications) {
        int nextSequence = applications.stream()
                .map(ApplicationRecord::getApplicationId)
                .mapToInt(this::extractApplicationSequence)
                .max()
                .orElse(0) + 1;
        return String.format("apply-%02d", nextSequence);
    }

    /**
     * Extracts the numeric sequence from an application id.
     */
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

    /**
     * Finds an older rejected or withdrawn record for the same applicant/job pair so
     * a resubmission can reuse that history.
     */
    private Optional<ApplicationRecord> findRejectedApplication(List<ApplicationRecord> applications,
                                                                String applicantId,
                                                                String jobId) {
        return applications.stream()
                .filter(existing -> applicantId.equals(existing.getApplicantId())
                        && jobId.equals(existing.getJobId())
                        && (existing.getStatus() == ApplicationStatus.REJECTED
                        || existing.getStatus() == ApplicationStatus.WITHDRAWN))
                        .findFirst();
    }

    /**
     * Synchronizes a job's open/closed state based on how many accepted applicants it has.
     */
    private void syncJobStatus(String jobId, List<ApplicationRecord> applications) {
        JobPosting job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        // The job stays OPEN until enough applicants are in ACCEPTED to satisfy the requested TA count.
        long acceptedCount = applications.stream()
                .filter(application -> jobId.equals(application.getJobId()))
                .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                .count();

        job.setStatus(acceptedCount >= job.getRequiredTaCount() ? JobStatus.CLOSED : JobStatus.OPEN);

        List<JobPosting> jobs = new ArrayList<>(jobRepository.findAll());
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getJobId().equals(job.getJobId())) {
                jobs.set(i, job);
                break;
            }
        }
        jobRepository.saveAll(jobs);
    }

    /**
     * Appends a note only once so repeated workflow actions do not duplicate text.
     */
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
