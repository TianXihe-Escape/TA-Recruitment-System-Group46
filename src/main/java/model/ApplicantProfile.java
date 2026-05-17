package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Applicant profile linked to a TA user account.
 * This model stores the profile information used for matching, review, and
 * contact purposes after the TA logs into the system.
 */
public class ApplicantProfile {
    /**
     * Unique applicant identifier used by applications and workload records.
     */
    private String applicantId;

    /**
     * Owning login-account id.
     */
    private String userId;

    /**
     * Applicant display name.
     */
    private String name;

    /**
     * Primary contact email.
     */
    private String email;

    /**
     * Applicant phone number.
     */
    private String phone;

    /**
     * Academic programme or major entered by the applicant.
     */
    private String programme;

    /**
     * Current year of study.
     */
    private String yearOfStudy;

    /**
     * Skills entered by the applicant for matching against job requirements.
     */
    private List<String> skills = new ArrayList<>();

    /**
     * Free-text summary of when the applicant can work.
     */
    private String availability;

    /**
     * Short experience description shown to reviewers.
     */
    private String experienceSummary;

    /**
     * Preferred TA duties or work types.
     */
    private String preferredDuties;

    /**
     * Local file path to the applicant CV selected in the UI.
     */
    private String cvPath;

    /**
     * Legacy single supporting document path. Kept for compatibility with older
     * JSON files and code paths that still expect one primary document.
     */
    private String supportingDocumentPath;

    /**
     * Optional supporting documents, such as transcripts, certificates, or proof
     * materials. The first entry is mirrored into supportingDocumentPath.
     */
    private List<String> supportingDocumentPaths = new ArrayList<>();

    /**
     * Jobs saved by the applicant for later review.
     */
    private List<String> favoriteJobIds = new ArrayList<>();

    /**
     * No-args constructor used by JSON deserialization and empty object creation.
     */
    public ApplicantProfile() {
    }

    /**
     * Creates a minimal profile bound to a user account.
     *
     * @param applicantId generated applicant id
     * @param userId owning user id
     */
    public ApplicantProfile(String applicantId, String userId) {
        this.applicantId = applicantId;
        this.userId = userId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public String getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(String yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        // Store a defensive copy so callers cannot mutate the internal list accidentally.
        this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public void setExperienceSummary(String experienceSummary) {
        this.experienceSummary = experienceSummary;
    }

    public String getPreferredDuties() {
        return preferredDuties;
    }

    public void setPreferredDuties(String preferredDuties) {
        this.preferredDuties = preferredDuties;
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    public String getSupportingDocumentPath() {
        return supportingDocumentPath;
    }

    public void setSupportingDocumentPath(String supportingDocumentPath) {
        String normalizedPath = supportingDocumentPath == null ? "" : supportingDocumentPath.trim();
        this.supportingDocumentPath = normalizedPath;
        this.supportingDocumentPaths = new ArrayList<>();
        if (!normalizedPath.isBlank()) {
            this.supportingDocumentPaths.add(normalizedPath);
        }
    }

    public List<String> getSupportingDocumentPaths() {
        return new ArrayList<>(supportingDocumentPaths);
    }

    public void setSupportingDocumentPaths(List<String> supportingDocumentPaths) {
        this.supportingDocumentPaths = new ArrayList<>();
        if (supportingDocumentPaths != null) {
            for (String documentPath : supportingDocumentPaths) {
                if (documentPath != null && !documentPath.trim().isBlank()) {
                    this.supportingDocumentPaths.add(documentPath.trim());
                }
            }
        }
        this.supportingDocumentPath = this.supportingDocumentPaths.isEmpty()
                ? ""
                : this.supportingDocumentPaths.get(0);
    }

    public List<String> getFavoriteJobIds() {
        return new ArrayList<>(favoriteJobIds);
    }

    public void setFavoriteJobIds(List<String> favoriteJobIds) {
        this.favoriteJobIds = favoriteJobIds == null ? new ArrayList<>() : new ArrayList<>(favoriteJobIds);
    }

    public boolean isFavoriteJob(String jobId) {
        return jobId != null && favoriteJobIds.contains(jobId);
    }
}
