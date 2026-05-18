package service;

import model.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service that turns accepted applications into workload-oriented admin data.
 * It is deliberately read-only: the service computes summaries and suggestions
 * but does not modify any persisted records.
 */
public class WorkloadService {
    private static final Pattern TIME_RANGE = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");

    /**
     * Builds workload summaries for every applicant using accepted applications only.
     *
     * @param profiles applicant profiles
     * @param jobs all job postings
     * @param applications all application records
     * @param threshold hour limit used to flag overload
     * @return workload records sorted by total hours descending
     */
    public List<WorkloadRecord> buildWorkloadRecords(List<ApplicantProfile> profiles,
                                                     List<JobPosting> jobs,
                                                     List<ApplicationRecord> applications,
                                                     int threshold) {
        List<WorkloadRecord> results = new ArrayList<>();

        for (ApplicantProfile profile : profiles) {
            WorkloadRecord record = new WorkloadRecord();
            record.setApplicantId(profile.getApplicantId());
            // Prefer the applicant's real name, but fall back to email so admin tables
            // still show an identifiable label when a profile is incomplete.
            record.setApplicantName(profile.getName() == null || profile.getName().isBlank()
                    ? profile.getEmail()
                    : profile.getName());
            record.setEmail(profile.getEmail());
            record.setThreshold(threshold);

            List<ApplicationRecord> accepted = applications.stream()
                    .filter(application -> application.getApplicantId().equals(profile.getApplicantId()))
                    .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                    .collect(Collectors.toList());

            int weeklyHours = 0;
            int oneOffHours = 0;
            List<String> jobIds = new ArrayList<>();
            List<String> modules = new ArrayList<>();
            List<String> weeklyModules = new ArrayList<>();
            List<String> oneOffModules = new ArrayList<>();
            List<JobPosting> acceptedJobs = new ArrayList<>();

            // Workload is based only on accepted jobs because shortlisted or submitted applications
            // should not count as a confirmed teaching commitment yet.
            for (ApplicationRecord application : accepted) {
                JobPosting job = jobs.stream()
                        .filter(item -> item.getJobId().equals(application.getJobId()))
                        .findFirst()
                        .orElse(null);
                if (job != null) {
                    if (job.isTotalWorkload()) {
                        oneOffHours += job.getHours();
                        oneOffModules.add(moduleLabel(job) + " (" + job.getHours() + "h total)");
                    } else {
                        weeklyHours += job.getHours();
                        weeklyModules.add(moduleLabel(job) + " (" + job.getHours() + "h/week)");
                    }
                    jobIds.add(job.getJobId());
                    modules.add(moduleLabel(job));
                    acceptedJobs.add(job);
                }
            }

            record.setAssignedJobIds(jobIds);
            record.setAssignedModules(modules);
            record.setWeeklyModules(weeklyModules);
            record.setOneOffModules(oneOffModules);
            record.setWeeklyHours(weeklyHours);
            record.setOneOffHours(oneOffHours);
            record.setOverload(weeklyHours > threshold);
            record.setWarningMessages(buildWarnings(record, acceptedJobs));
            results.add(record);
        }

        // Highest-load applicants are shown first because overload investigation is the
        // main admin use case for this screen.
        return results.stream()
                .sorted(Comparator.comparing(WorkloadRecord::getWeeklyHours).reversed()
                        .thenComparing(Comparator.comparing(WorkloadRecord::getOneOffHours).reversed()))
                .collect(Collectors.toList());
    }

    /**
     * Suggests the top applicants for a job by combining skill match and current load.
     *
     * @param jobPosting target job
     * @param profiles all applicants
     * @param workloadRecords precomputed workloads
     * @param matchingService skill matching helper
     * @return up to three suggestion strings for admin display
     */
    public List<String> suggestApplicantsForJob(JobPosting jobPosting,
                                                List<ApplicantProfile> profiles,
                                                List<WorkloadRecord> workloadRecords,
                                                MatchingService matchingService) {
        return profiles.stream()
                .sorted((left, right) -> {
                    int leftScore = matchingService.calculateMatch(left.getSkills(), jobPosting.getRequiredSkills()).getScorePercentage();
                    int rightScore = matchingService.calculateMatch(right.getSkills(), jobPosting.getRequiredSkills()).getScorePercentage();
                    if (leftScore != rightScore) {
                        // Prefer skill fit first so recommendations stay aligned with the MO's job requirements.
                        return Integer.compare(rightScore, leftScore);
                    }
                    int leftHours = findHours(left.getApplicantId(), workloadRecords);
                    int rightHours = findHours(right.getApplicantId(), workloadRecords);
                    // Break ties with lower current load so recommendations also support balancing.
                    return Integer.compare(leftHours, rightHours);
                })
                .limit(3)
                .map(profile -> {
                    // Recompute the score for the final display string so the explanation
                    // shown to admins always matches the ranking criteria above.
                    int score = matchingService.calculateMatch(profile.getSkills(), jobPosting.getRequiredSkills()).getScorePercentage();
                    int hours = findHours(profile.getApplicantId(), workloadRecords);
                    return profile.getName() + " (" + score + "% match, " + hours + "h/week current load)";
                })
                .collect(Collectors.toList());
    }

    public int projectedHours(String applicantId, JobPosting jobPosting, List<WorkloadRecord> workloadRecords) {
        return findHours(applicantId, workloadRecords) + weeklyHoursFor(jobPosting);
    }

    public boolean wouldExceedThreshold(String applicantId,
                                        JobPosting jobPosting,
                                        List<WorkloadRecord> workloadRecords,
                                        int threshold) {
        return projectedHours(applicantId, jobPosting, workloadRecords) > threshold;
    }

    /**
     * Looks up current assigned hours for a specific applicant from prebuilt workload records.
     */
    private int findHours(String applicantId, List<WorkloadRecord> records) {
        return records.stream()
                .filter(record -> record.getApplicantId().equals(applicantId))
                .map(WorkloadRecord::getWeeklyHours)
                .findFirst()
                .orElse(0);
    }

    private int weeklyHoursFor(JobPosting jobPosting) {
        return jobPosting == null || jobPosting.isTotalWorkload() ? 0 : jobPosting.getHours();
    }

    private String moduleLabel(JobPosting job) {
        return job.getModuleCode() + " " + job.getModuleTitle();
    }

    private List<String> buildWarnings(WorkloadRecord record, List<JobPosting> acceptedJobs) {
        List<String> warnings = new ArrayList<>();
        if (record.isOverload()) {
            warnings.add("Over weekly threshold");
        }
        if (!record.isOverload() && record.getOneOffHours() >= 4) {
            warnings.add("Check event workload");
        }
        List<String> conflicts = findScheduleConflicts(acceptedJobs);
        if (!conflicts.isEmpty()) {
            // Keep both the summary flag and the human-readable messages for different admin views.
            record.setScheduleConflict(true);
            warnings.addAll(conflicts);
        }
        if (warnings.isEmpty()) {
            warnings.add("Normal");
        }
        return warnings;
    }

    public List<String> findScheduleConflicts(List<JobPosting> acceptedJobs) {
        List<String> conflicts = new ArrayList<>();
        for (int i = 0; i < acceptedJobs.size(); i++) {
            JobPosting left = acceptedJobs.get(i);
            for (int j = i + 1; j < acceptedJobs.size(); j++) {
                JobPosting right = acceptedJobs.get(j);
                if (!sameDate(left, right)) {
                    continue;
                }
                // This lightweight check only understands same-day time ranges like "09:00 - 11:00".
                Optional<TimeRange> leftRange = parseTimeRange(left.getSchedule());
                Optional<TimeRange> rightRange = parseTimeRange(right.getSchedule());
                if (leftRange.isPresent() && rightRange.isPresent() && leftRange.get().overlaps(rightRange.get())) {
                    conflicts.add("Schedule conflict: " + left.getModuleCode() + " overlaps with " + right.getModuleCode());
                }
            }
        }
        return conflicts;
    }

    private boolean sameDate(JobPosting left, JobPosting right) {
        String leftDate = left.getStartDate();
        String rightDate = right.getStartDate();
        return leftDate != null && !leftDate.isBlank() && leftDate.equals(rightDate);
    }

    private Optional<TimeRange> parseTimeRange(String schedule) {
        if (schedule == null || schedule.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = TIME_RANGE.matcher(schedule);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new TimeRange(LocalTime.parse(matcher.group(1)), LocalTime.parse(matcher.group(2))));
        } catch (RuntimeException ex) {
            // Free-text schedules should degrade gracefully rather than break the workload screen.
            return Optional.empty();
        }
    }

    private record TimeRange(LocalTime start, LocalTime end) {
        boolean overlaps(TimeRange other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }
}
