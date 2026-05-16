package service;

import model.AllocationRecord;
import model.ApplicationRecord;
import repository.AllocationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maintains allocation records derived from accepted applications.
 */
public class AllocationService {
    private static final Pattern ALLOCATION_ID_PATTERN = Pattern.compile("^alloc-(\\d+)$");

    private final AllocationRepository allocationRepository;

    public AllocationService(AllocationRepository allocationRepository) {
        this.allocationRepository = allocationRepository;
    }

    public void activateAllocation(ApplicationRecord application, String actorUserId) {
        if (application == null || application.getApplicationId() == null) {
            return;
        }
        List<AllocationRecord> allocations = new ArrayList<>(allocationRepository.findAll());
        AllocationRecord allocation = allocations.stream()
                .filter(item -> application.getApplicationId().equals(item.getApplicationId()))
                .findFirst()
                .orElse(null);
        if (allocation == null) {
            allocation = new AllocationRecord();
            allocation.setAllocationId(nextAllocationId(allocations));
            allocation.setApplicationId(application.getApplicationId());
            allocation.setApplicantId(application.getApplicantId());
            allocation.setJobId(application.getJobId());
            allocations.add(allocation);
        }
        allocation.setAllocatedByUserId(actorUserId);
        allocation.setAllocatedAt(LocalDateTime.now());
        allocation.setActive(true);
        allocationRepository.saveAll(allocations);
    }

    public void deactivateAllocationForApplication(String applicationId) {
        List<AllocationRecord> allocations = new ArrayList<>(allocationRepository.findAll());
        boolean updated = false;
        for (AllocationRecord allocation : allocations) {
            if (applicationId != null && applicationId.equals(allocation.getApplicationId()) && allocation.isActive()) {
                allocation.setActive(false);
                updated = true;
            }
        }
        if (updated) {
            allocationRepository.saveAll(allocations);
        }
    }

    public void deactivateAllocationsForApplicant(String applicantId) {
        List<AllocationRecord> allocations = new ArrayList<>(allocationRepository.findAll());
        boolean updated = false;
        for (AllocationRecord allocation : allocations) {
            if (applicantId != null && applicantId.equals(allocation.getApplicantId()) && allocation.isActive()) {
                allocation.setActive(false);
                updated = true;
            }
        }
        if (updated) {
            allocationRepository.saveAll(allocations);
        }
    }

    private String nextAllocationId(List<AllocationRecord> allocations) {
        int next = allocations.stream()
                .map(AllocationRecord::getAllocationId)
                .mapToInt(this::extractSequence)
                .max()
                .orElse(0) + 1;
        return String.format("alloc-%02d", next);
    }

    private int extractSequence(String allocationId) {
        if (allocationId == null) {
            return 0;
        }
        Matcher matcher = ALLOCATION_ID_PATTERN.matcher(allocationId.trim());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
