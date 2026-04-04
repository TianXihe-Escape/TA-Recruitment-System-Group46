package service;

import model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregates accepted applications into admin workload views.
 */
public class WorkloadService {
    public List<WorkloadRecord> buildWorkloadRecords(List<ApplicantProfile> profiles,
                                                     List<JobPosting> jobs,
                                                     List<ApplicationRecord> applications,
                                                     int threshold) {
        List<WorkloadRecord> results = new ArrayList<>();

        for (ApplicantProfile profile : profiles) {
            WorkloadRecord record = new WorkloadRecord();
            record.setApplicantId(profile.getApplicantId());
            record.setApplicantName(profile.getName() == null || profile.getName().isBlank()
                    ? profile.getEmail()
                    : profile.getName());

            List<ApplicationRecord> accepted = applications.stream()
                    .filter(application -> application.getApplicantId().equals(profile.getApplicantId()))
                    .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                    .collect(Collectors.toList());

            int totalHours = 0;
            List<String> jobIds = new ArrayList<>();
            List<String> modules = new ArrayList<>();

            for (ApplicationRecord application : accepted) {
                JobPosting job = jobs.stream()
                        .filter(item -> item.getJobId().equals(application.getJobId()))
                        .findFirst()
                        .orElse(null);
                if (job != null) {
                    totalHours += job.getHours();
                    jobIds.add(job.getJobId());
                    modules.add(job.getModuleCode() + " " + job.getModuleTitle());
                }
            }

            record.setAssignedJobIds(jobIds);
            record.setAssignedModules(modules);
            record.setTotalHours(totalHours);
            record.setOverload(totalHours > threshold);
            results.add(record);
        }

        return results.stream()
                .sorted(Comparator.comparing(WorkloadRecord::getTotalHours).reversed())
                .collect(Collectors.toList());
    }

    public List<String> suggestApplicantsForJob(JobPosting jobPosting,
                                                List<ApplicantProfile> profiles,
                                                List<WorkloadRecord> workloadRecords,
                                                MatchingService matchingService) {
        return profiles.stream()
                .sorted((left, right) -> {
                    int leftScore = matchingService.calculateMatch(left.getSkills(), jobPosting.getRequiredSkills()).getScorePercentage();
                    int rightScore = matchingService.calculateMatch(right.getSkills(), jobPosting.getRequiredSkills()).getScorePercentage();
                    if (leftScore != rightScore) {
                        return Integer.compare(rightScore, leftScore);
                    }
                    int leftHours = findHours(left.getApplicantId(), workloadRecords);
                    int rightHours = findHours(right.getApplicantId(), workloadRecords);
                    return Integer.compare(leftHours, rightHours);
                })
                .limit(3)
                .map(profile -> {
                    int score = matchingService.calculateMatch(profile.getSkills(), jobPosting.getRequiredSkills()).getScorePercentage();
                    int hours = findHours(profile.getApplicantId(), workloadRecords);
                    return profile.getName() + " (" + score + "% match, " + hours + "h current load)";
                })
                .collect(Collectors.toList());
    }

    private int findHours(String applicantId, List<WorkloadRecord> records) {
        return records.stream()
                .filter(record -> record.getApplicantId().equals(applicantId))
                .map(WorkloadRecord::getTotalHours)
                .findFirst()
                .orElse(0);
    }
}
