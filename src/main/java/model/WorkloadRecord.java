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
    private String email;
    private List<String> assignedJobIds = new ArrayList<>();
    private List<String> assignedModules = new ArrayList<>();
    private List<String> weeklyModules = new ArrayList<>();
    private List<String> oneOffModules = new ArrayList<>();
    private int totalHours;
    private int weeklyHours;
    private int oneOffHours;
    private int threshold;
    private boolean overload;
    private boolean scheduleConflict;
    private List<String> warningMessages = new ArrayList<>();

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public List<String> getWeeklyModules() {
        return weeklyModules;
    }

    public void setWeeklyModules(List<String> weeklyModules) {
        this.weeklyModules = weeklyModules == null ? new ArrayList<>() : new ArrayList<>(weeklyModules);
    }

    public List<String> getOneOffModules() {
        return oneOffModules;
    }

    public void setOneOffModules(List<String> oneOffModules) {
        this.oneOffModules = oneOffModules == null ? new ArrayList<>() : new ArrayList<>(oneOffModules);
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }

    public int getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(int weeklyHours) {
        this.weeklyHours = weeklyHours;
        this.totalHours = weeklyHours;
    }

    public int getOneOffHours() {
        return oneOffHours;
    }

    public void setOneOffHours(int oneOffHours) {
        this.oneOffHours = oneOffHours;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public boolean isOverload() {
        return overload;
    }

    public void setOverload(boolean overload) {
        this.overload = overload;
    }

    public boolean isScheduleConflict() {
        return scheduleConflict;
    }

    public void setScheduleConflict(boolean scheduleConflict) {
        this.scheduleConflict = scheduleConflict;
    }

    public List<String> getWarningMessages() {
        return warningMessages;
    }

    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages == null ? new ArrayList<>() : new ArrayList<>(warningMessages);
    }

    public String getWarningMessage() {
        if (warningMessages.isEmpty()) {
            return overload ? "Over weekly threshold" : "Normal";
        }
        return String.join("; ", warningMessages);
    }
}
