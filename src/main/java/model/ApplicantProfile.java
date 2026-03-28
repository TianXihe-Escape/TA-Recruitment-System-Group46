package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Applicant profile owned by a TA account.
 */
public class ApplicantProfile {
    private String applicantId;
    private String userId;
    private String name;
    private String email;
    private String phone;
    private List<String> skills = new ArrayList<>();
    private String availability;
    private String experienceSummary;
    private String preferredDuties;
    private String cvPath;

    public ApplicantProfile() {
    }

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

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
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
}
