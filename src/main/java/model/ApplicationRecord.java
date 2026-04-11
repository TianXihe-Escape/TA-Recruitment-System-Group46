package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent record of a TA application to a specific job posting.
 * Each record tracks both the workflow status and lightweight review metadata
 * such as missing skills and reviewer notes.
 */
public class ApplicationRecord {
    /**
     * Unique application identifier.
     */
    private String applicationId;

    /**
     * Applicant who submitted the application.
     */
    private String applicantId;

    /**
     * Job posting being applied to.
     */
    private String jobId;

    /**
     * Timestamp of the latest submission or resubmission.
     */
    private LocalDateTime appliedAt;

    /**
     * Current state in the review workflow.
     */
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    /**
     * Notes left by reviewers or system workflow transitions.
     */
    private String reviewerNotes;

    /**
     * Skill match score calculated at submission time.
     */
    private int matchScore;

    /**
     * Skills required by the job that were not matched by the applicant.
     */
    private List<String> missingSkills = new ArrayList<>();

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getReviewerNotes() {
        return reviewerNotes;
    }

    public void setReviewerNotes(String reviewerNotes) {
        this.reviewerNotes = reviewerNotes;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        // Keep a copy so outside callers cannot hold a mutable reference to internal state.
        this.missingSkills = missingSkills == null ? new ArrayList<>() : new ArrayList<>(missingSkills);
    }
}
