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
     * Timestamp of the latest workflow update.
     */
    private LocalDateTime lastUpdatedAt;

    /**
     * Timestamp of the final accept/reject decision, if one exists.
     */
    private LocalDateTime decisionAt;

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

    /**
     * Ordered audit trail of status changes.
     */
    private List<StatusHistoryEntry> statusHistory = new ArrayList<>();

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

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public LocalDateTime getDecisionAt() {
        return decisionAt;
    }

    public void setDecisionAt(LocalDateTime decisionAt) {
        this.decisionAt = decisionAt;
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

    public List<StatusHistoryEntry> getStatusHistory() {
        return new ArrayList<>(statusHistory);
    }

    public void setStatusHistory(List<StatusHistoryEntry> statusHistory) {
        this.statusHistory = statusHistory == null ? new ArrayList<>() : new ArrayList<>(statusHistory);
    }

    public void addStatusHistory(StatusHistoryEntry entry) {
        if (entry != null) {
            statusHistory.add(entry);
        }
    }
}
