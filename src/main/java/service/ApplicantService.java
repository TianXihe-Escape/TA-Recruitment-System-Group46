package service;

import model.ApplicantProfile;
import repository.ApplicantProfileRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages TA profile editing and CV metadata.
 */
public class ApplicantService {
    private final ApplicantProfileRepository profileRepository;
    private final ValidationService validationService;

    public ApplicantService(ApplicantProfileRepository profileRepository, ValidationService validationService) {
        this.profileRepository = profileRepository;
        this.validationService = validationService;
    }

    public ApplicantProfile getProfileByUserId(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Applicant profile not found."));
    }

    public ApplicantProfile getProfileByApplicantId(String applicantId) {
        return profileRepository.findByApplicantId(applicantId)
                .orElseThrow(() -> new IllegalStateException("Applicant profile not found."));
    }

    public void saveProfile(ApplicantProfile updatedProfile) {
        updatedProfile.setName(validationService.normalizePersonName(updatedProfile.getName()));
        updatedProfile.setEmail(validationService.normalizeEmail(updatedProfile.getEmail()));
        updatedProfile.setPhone(validationService.normalizePhone(updatedProfile.getPhone()));
        updatedProfile.setAvailability(validationService.normalizeText(updatedProfile.getAvailability()));
        updatedProfile.setPreferredDuties(validationService.normalizeText(updatedProfile.getPreferredDuties()));
        updatedProfile.setExperienceSummary(validationService.normalizeMultilineText(updatedProfile.getExperienceSummary()));

        List<String> errors = validationService.validateApplicantProfile(
                updatedProfile.getName(),
                updatedProfile.getEmail(),
                updatedProfile.getPhone()
        );
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        List<ApplicantProfile> profiles = new ArrayList<>(profileRepository.findAll());
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getApplicantId().equals(updatedProfile.getApplicantId())) {
                profiles.set(i, updatedProfile);
                profileRepository.saveAll(profiles);
                return;
            }
        }
        throw new IllegalStateException("Applicant profile not found for save.");
    }
}
