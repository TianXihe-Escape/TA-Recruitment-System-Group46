package service;

import model.JobPosting;
import model.JobStatus;
import repository.JobRepository;
import util.IdGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that owns job-posting level business rules.
 * It provides a thin layer above the repository for sorting, filtering, id
 * generation, and validation before jobs are saved to disk.
 */
public class JobService {
    /**
     * Repository for persistent job-posting storage.
     */
    private final JobRepository jobRepository;

    /**
     * Validation helper used to normalize module and skills input.
     */
    private final ValidationService validationService;

    /**
     * Creates the job service with its dependencies.
     *
     * @param jobRepository repository for job records
     * @param validationService helper for normalization and validation logic
     */
    public JobService(JobRepository jobRepository, ValidationService validationService) {
        this.jobRepository = jobRepository;
        this.validationService = validationService;
    }

    /**
     * Returns every job in a predictable module-code order for table rendering.
     *
     * @return sorted list of all jobs
     */
    public List<JobPosting> getAllJobs() {
        return jobRepository.findAll().stream()
                .sorted(Comparator.comparing(JobPosting::getModuleCode))
                .collect(Collectors.toList());
    }

    /**
     * Returns only open jobs that applicants can still apply to.
     *
     * @return filtered list of jobs whose status is {@link JobStatus#OPEN}
     */
    public List<JobPosting> getOpenJobs() {
        return getAllJobs().stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .collect(Collectors.toList());
    }

    /**
     * Finds a single job by id.
     *
     * @param jobId target job id
     * @return job posting with the requested id
     */
    public JobPosting getJobById(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));
    }

    /**
     * Saves a job posting after normalizing fields and validating business rules.
     * New jobs receive a generated id while existing jobs are updated in place.
     *
     * @param jobPosting job to create or update
     */
    public void saveJob(JobPosting jobPosting) {
        // Normalize user-entered text before validating so stored data stays consistent
        // even when different MOs type the same information in different formats.
        jobPosting.setModuleCode(validationService.normalizeModuleCode(jobPosting.getModuleCode()));
        jobPosting.setModuleTitle(validationService.normalizeText(jobPosting.getModuleTitle()));
        jobPosting.setDuties(validationService.normalizeMultilineText(jobPosting.getDuties()));
        jobPosting.setRequiredSkills(validationService.parseSkills(String.join(", ", jobPosting.getRequiredSkills())));

        List<String> errors = validationService.validateJobPosting(jobPosting);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        List<JobPosting> jobs = new ArrayList<>(jobRepository.findAll());
        if (jobPosting.getJobId() == null || jobPosting.getJobId().isBlank()) {
            // Generate ids from the current data set so manually seeded demo data
            // and newly created jobs still share the same numbering scheme.
            jobPosting.setJobId(IdGenerator.nextJobId(
                    jobs.stream()
                            .map(JobPosting::getJobId)
                            .toList()
            ));
            jobs.add(jobPosting);
        } else {
            boolean updated = false;
            for (int i = 0; i < jobs.size(); i++) {
                if (jobs.get(i).getJobId().equals(jobPosting.getJobId())) {
                    // Update in place so the saved JSON list keeps a stable order.
                    jobs.set(i, jobPosting);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                // Fall back to append if the caller supplied an id that is not currently stored.
                jobs.add(jobPosting);
            }
        }
        jobRepository.saveAll(jobs);
    }
}
