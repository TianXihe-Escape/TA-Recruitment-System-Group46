package service;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.JobPosting;
import model.WorkloadRecord;
import util.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes recruitment summaries to CSV files for external review.
 */
public class ExportService {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public Path exportRecruitmentReport(List<ApplicantProfile> profiles,
                                        List<JobPosting> jobs,
                                        List<ApplicationRecord> applications,
                                        List<WorkloadRecord> workloads) {
        try {
            Files.createDirectories(Constants.EXPORTS_DIR);
            Path output = Constants.EXPORTS_DIR.resolve("recruitment-report-" + FILE_TIMESTAMP.format(LocalDateTime.now()) + ".csv");
            Files.writeString(output, buildCsv(profiles, jobs, applications, workloads), StandardCharsets.UTF_8);
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Could not export recruitment report:\n" + e.getMessage(), e);
        }
    }

    private String buildCsv(List<ApplicantProfile> profiles,
                            List<JobPosting> jobs,
                            List<ApplicationRecord> applications,
                            List<WorkloadRecord> workloads) {
        StringBuilder builder = new StringBuilder();
        builder.append("Section,ID,Name,Related ID,Status,Hours,Category,Semester,Details\n");
        for (JobPosting job : jobs) {
            long applicationCount = applications.stream()
                    .filter(application -> job.getJobId().equals(application.getJobId()))
                    .count();
            builder.append(row(
                    "Job",
                    job.getJobId(),
                    job.getModuleCode() + " " + job.getModuleTitle(),
                    job.getPostedBy(),
                    String.valueOf(job.getStatus()),
                    String.valueOf(job.getHours()),
                    job.getCategory() == null ? "" : job.getCategory().getDisplayName(),
                    job.getSemester(),
                    "applications=" + applicationCount
                            + "; requiredTAs=" + job.getRequiredTaCount()
                            + "; jobType=" + job.getJobType()
                            + "; workloadType=" + job.getWorkloadType()
                            + "; period=" + job.getStartDate() + " to " + job.getEndDate()
                            + "; schedule=" + job.getSchedule()
                            + "; location=" + job.getLocation()
            ));
        }
        for (ApplicationRecord application : applications) {
            builder.append(row(
                    "Application",
                    application.getApplicationId(),
                    applicantName(profiles, application.getApplicantId()),
                    application.getJobId(),
                    String.valueOf(application.getStatus()),
                    "",
                    "",
                    "",
                    "match=" + application.getMatchScore() + "%; missing=" + String.join("|", application.getMissingSkills())
            ));
        }
        for (WorkloadRecord workload : workloads) {
            builder.append(row(
                    "Workload",
                    workload.getApplicantId(),
                    workload.getApplicantName(),
                    String.join("|", workload.getAssignedJobIds()),
                    workload.isOverload() ? "OVERLOAD" : "OK",
                    workload.getWeeklyHours() + "h/week",
                    "",
                    "",
                    "weekly=" + String.join("|", workload.getWeeklyModules())
                            + "; oneOff=" + workload.getOneOffHours() + "h total "
                            + String.join("|", workload.getOneOffModules())
            ));
        }
        return builder.toString();
    }

    private String applicantName(List<ApplicantProfile> profiles, String applicantId) {
        return profiles.stream()
                .filter(profile -> applicantId != null && applicantId.equals(profile.getApplicantId()))
                .map(ApplicantProfile::getName)
                .findFirst()
                .orElse(applicantId == null ? "" : applicantId);
    }

    private String row(String... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escape(values[i]));
        }
        builder.append('\n');
        return builder.toString();
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
