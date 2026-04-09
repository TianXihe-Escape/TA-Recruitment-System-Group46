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
 * Coordinates job creation and retrieval.
 */
public class JobService {
    private final JobRepository jobRepository;
    private final ValidationService validationService;

    public JobService(JobRepository jobRepository, ValidationService validationService) {
        this.jobRepository = jobRepository;
        this.validationService = validationService;
    }

    public List<JobPosting> getAllJobs() {
        return jobRepository.findAll().stream()
                .sorted(Comparator.comparing(JobPosting::getModuleCode))
                .collect(Collectors.toList());
    }

    public List<JobPosting> getOpenJobs() {
        return getAllJobs().stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .collect(Collectors.toList());
    }

    public JobPosting getJobById(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));
    }

    public void saveJob(JobPosting jobPosting) {
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
            jobPosting.setJobId(IdGenerator.newId("job"));
            jobs.add(jobPosting);
        } else {
            boolean updated = false;
            for (int i = 0; i < jobs.size(); i++) {
                if (jobs.get(i).getJobId().equals(jobPosting.getJobId())) {
                    jobs.set(i, jobPosting);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                jobs.add(jobPosting);
            }
        }
        jobRepository.saveAll(jobs);
    }
}
