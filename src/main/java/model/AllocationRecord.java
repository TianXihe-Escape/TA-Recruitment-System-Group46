package model;

import java.time.LocalDateTime;

/**
 * Persistent allocation created when an application is accepted.
 */
public class AllocationRecord {
    private String allocationId;
    private String applicationId;
    private String applicantId;
    private String jobId;
    private String allocatedByUserId;
    private LocalDateTime allocatedAt;
    private boolean active = true;

    public String getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(String allocationId) {
        this.allocationId = allocationId;
    }

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

    public String getAllocatedByUserId() {
        return allocatedByUserId;
    }

    public void setAllocatedByUserId(String allocatedByUserId) {
        this.allocatedByUserId = allocatedByUserId;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
