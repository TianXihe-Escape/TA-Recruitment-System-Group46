package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TA vacancy published by a module organiser.
 */
public class JobPosting {
    public static final String JOB_TYPE_COURSE_SUPPORT = "COURSE_SUPPORT";
    public static final String JOB_TYPE_INVIGILATION = "INVIGILATION";
    public static final String JOB_TYPE_DEMO_SUPPORT = "DEMO_SUPPORT";
    public static final String JOB_TYPE_WORKSHOP_SUPPORT = "WORKSHOP_SUPPORT";
    public static final String JOB_TYPE_OTHER = "OTHER";
    public static final String WORKLOAD_TYPE_WEEKLY = "WEEKLY";
    public static final String WORKLOAD_TYPE_TOTAL = "TOTAL";

    private String jobId;
    private String moduleCode;
    private String moduleTitle;
    private JobCategory category = JobCategory.MODULE_TA;
    private String semester;
    private String duties;
    private int hours;
    private String jobType = JOB_TYPE_COURSE_SUPPORT;
    private String startDate = "";
    private String endDate = "";
    private String schedule = "";
    private String location = "";
    private String workloadType = WORKLOAD_TYPE_WEEKLY;
    private int requiredTaCount = 1;
    private List<String> requiredSkills = new ArrayList<>();
    private LocalDate applicationDeadline;
    private JobStatus status = JobStatus.OPEN;
    private String postedBy;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleTitle() {
        return moduleTitle;
    }

    public void setModuleTitle(String moduleTitle) {
        this.moduleTitle = moduleTitle;
    }

    public JobCategory getCategory() {
        return category;
    }

    public void setCategory(JobCategory category) {
        this.category = category == null ? JobCategory.MODULE_TA : category;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getDuties() {
        return duties;
    }

    public void setDuties(String duties) {
        this.duties = duties;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = normalizeJobType(jobType);
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate == null ? "" : startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate == null ? "" : endDate;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule == null ? "" : schedule;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location == null ? "" : location;
    }

    public String getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(String workloadType) {
        this.workloadType = normalizeWorkloadType(workloadType);
    }

    public boolean isWeeklyWorkload() {
        return WORKLOAD_TYPE_WEEKLY.equals(workloadType);
    }

    public boolean isTotalWorkload() {
        return WORKLOAD_TYPE_TOTAL.equals(workloadType);
    }

    public int getRequiredTaCount() {
        return requiredTaCount;
    }

    public void setRequiredTaCount(int requiredTaCount) {
        this.requiredTaCount = requiredTaCount;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : new ArrayList<>(requiredSkills);
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    private String normalizeJobType(String value) {
        if (value == null || value.isBlank()) {
            return JOB_TYPE_COURSE_SUPPORT;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case JOB_TYPE_INVIGILATION, JOB_TYPE_DEMO_SUPPORT, JOB_TYPE_WORKSHOP_SUPPORT, JOB_TYPE_OTHER -> normalized;
            default -> JOB_TYPE_COURSE_SUPPORT;
        };
    }

    private String normalizeWorkloadType(String value) {
        if (value == null || value.isBlank()) {
            return WORKLOAD_TYPE_WEEKLY;
        }
        String normalized = value.trim().toUpperCase();
        return WORKLOAD_TYPE_TOTAL.equals(normalized) ? WORKLOAD_TYPE_TOTAL : WORKLOAD_TYPE_WEEKLY;
    }
}
