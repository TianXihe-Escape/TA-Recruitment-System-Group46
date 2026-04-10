package service;

import model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that turns accepted applications into workload-oriented admin data.
 * It is deliberately read-only: the service computes summaries and suggestions
 * but does not modify any persisted records.
 */
public class WorkloadService {
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

            List<ApplicationRecord> accepted = applications.stream()
                    .filter(application -> application.getApplicantId().equals(profile.getApplicantId()))
                    .filter(application -> application.getStatus() == ApplicationStatus.ACCEPTED)
                    .collect(Collectors.toList());

            int totalHours = 0;
            List<String> jobIds = new ArrayList<>();
            List<String> modules = new ArrayList<>();

            // Workload is based only on accepted jobs because shortlisted or submitted applications
            // should not count as a confirmed teaching commitment yet.
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

        // Highest-load applicants are shown first because overload investigation is the
        // main admin use case for this screen.
        return results.stream()
                .sorted(Comparator.comparing(WorkloadRecord::getTotalHours).reversed())
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
                    return profile.getName() + " (" + score + "% match, " + hours + "h current load)";
                })
                .collect(Collectors.toList());
    }

    /**
     * Looks up current assigned hours for a specific applicant from prebuilt workload records.
     */
    private int findHours(String applicantId, List<WorkloadRecord> records) {
        return records.stream()
                .filter(record -> record.getApplicantId().equals(applicantId))
                .map(WorkloadRecord::getTotalHours)
                .findFirst()
                .orElse(0);
    }
}
