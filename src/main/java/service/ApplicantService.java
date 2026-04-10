package service;

import model.ApplicantProfile;
import repository.ApplicantProfileRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for reading and updating applicant-facing profile data.
 * This layer keeps profile validation and normalization out of the Swing UI so
 * frames can focus on interaction logic instead of persistence rules.
 */
public class ApplicantService {
    /**
     * Repository that persists applicant profile records in the JSON store.
     */
    private final ApplicantProfileRepository profileRepository;

    /**
     * Shared validation helper used to normalize profile input before saving.
     */
    private final ValidationService validationService;

    /**
     * Creates the profile service with its required collaborators.
     *
     * @param profileRepository repository used to load and save applicant profiles
     * @param validationService helper used for normalization and validation rules
     */
    public ApplicantService(ApplicantProfileRepository profileRepository, ValidationService validationService) {
        this.profileRepository = profileRepository;
        this.validationService = validationService;
    }

    /**
     * Loads the applicant profile that belongs to a specific login account.
     *
     * @param userId owning user id
     * @return matching applicant profile
     */
    public ApplicantProfile getProfileByUserId(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Applicant profile not found."));
    }

    /**
     * Loads a profile directly by applicant id.
     *
     * @param applicantId applicant identifier stored in applications
     * @return matching applicant profile
     */
    public ApplicantProfile getProfileByApplicantId(String applicantId) {
        return profileRepository.findByApplicantId(applicantId)
                .orElseThrow(() -> new IllegalStateException("Applicant profile not found."));
    }

    /**
     * Normalizes, validates, and persists an updated applicant profile.
     * The method overwrites the existing stored profile with the same applicant id.
     *
     * @param updatedProfile profile object coming from the UI
     */
    public void saveProfile(ApplicantProfile updatedProfile) {
        // Normalize free-text fields first so validation and persistence operate on
        // the canonical representation shown back to the user later.
        updatedProfile.setName(validationService.normalizePersonName(updatedProfile.getName()));
        updatedProfile.setEmail(validationService.normalizeEmail(updatedProfile.getEmail()));
        updatedProfile.setPhone(validationService.normalizePhone(updatedProfile.getPhone()));
        updatedProfile.setAvailability(validationService.normalizeText(updatedProfile.getAvailability()));
        updatedProfile.setPreferredDuties(validationService.normalizeText(updatedProfile.getPreferredDuties()));
        updatedProfile.setExperienceSummary(validationService.normalizeMultilineText(updatedProfile.getExperienceSummary()));

        // Aggregate all validation failures so the UI can display them in one message
        // instead of forcing the applicant to fix issues one at a time.
        List<String> errors = validationService.validateApplicantProfile(
                updatedProfile.getName(),
                updatedProfile.getEmail(),
                updatedProfile.getPhone()
        );
        errors.addAll(validationService.validateCvPath(updatedProfile.getCvPath()));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        List<ApplicantProfile> profiles = new ArrayList<>(profileRepository.findAll());
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getApplicantId().equals(updatedProfile.getApplicantId())) {
                // Replace the existing record in place to preserve list order in the JSON file.
                profiles.set(i, updatedProfile);
                profileRepository.saveAll(profiles);
                return;
            }
        }
        throw new IllegalStateException("Applicant profile not found for save.");
    }
}
