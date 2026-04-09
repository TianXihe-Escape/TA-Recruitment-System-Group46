package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated view of accepted TA workload.
 * This model is used only for admin reporting and suggestion logic, not for persistence.
 */
public class WorkloadRecord {
    private String applicantId;
    private String applicantName;
    private List<String> assignedJobIds = new ArrayList<>();
    private List<String> assignedModules = new ArrayList<>();
    private int totalHours;
    private boolean overload;

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public List<String> getAssignedJobIds() {
        return assignedJobIds;
    }

    public void setAssignedJobIds(List<String> assignedJobIds) {
        this.assignedJobIds = assignedJobIds == null ? new ArrayList<>() : new ArrayList<>(assignedJobIds);
    }

    public List<String> getAssignedModules() {
        return assignedModules;
    }

    public void setAssignedModules(List<String> assignedModules) {
        this.assignedModules = assignedModules == null ? new ArrayList<>() : new ArrayList<>(assignedModules);
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }

    public boolean isOverload() {
        return overload;
    }

    public void setOverload(boolean overload) {
        this.overload = overload;
    }
}
