package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TA vacancy published by a module organiser.
 */
public class JobPosting {
    private String jobId;
    private String moduleCode;
    private String moduleTitle;
    private JobCategory category = JobCategory.MODULE_TA;
    private String semester;
    private String duties;
    private int hours;
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
}
