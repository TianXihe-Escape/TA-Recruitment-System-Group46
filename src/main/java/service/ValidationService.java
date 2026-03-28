package service;

import model.JobPosting;
import util.FileUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Central place for validating user input and forms.
 */
public class ValidationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public List<String> validateRegistration(String username, String password, String confirmPassword) {
        List<String> errors = new ArrayList<>();
        if (FileUtil.isBlank(username)) {
            errors.add("Email/username is required.");
        } else if (!EMAIL_PATTERN.matcher(username.trim()).matches()) {
            errors.add("Please enter a valid email address.");
        }
        if (FileUtil.isBlank(password)) {
            errors.add("Password is required.");
        } else if (password.length() < 4) {
            errors.add("Password must be at least 4 characters.");
        }
        if (!String.valueOf(password).equals(confirmPassword)) {
            errors.add("Passwords do not match.");
        }
        return errors;
    }

    public List<String> validateLogin(String username, String password) {
        List<String> errors = new ArrayList<>();
        if (FileUtil.isBlank(username)) {
            errors.add("Username is required.");
        }
        if (FileUtil.isBlank(password)) {
            errors.add("Password is required.");
        }
        return errors;
    }

    public List<String> validateApplicantProfile(String name, String email, String phone) {
        List<String> errors = new ArrayList<>();
        if (FileUtil.isBlank(name)) {
            errors.add("Name is required.");
        }
        if (FileUtil.isBlank(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            errors.add("A valid email is required.");
        }
        if (FileUtil.isBlank(phone)) {
            errors.add("Phone number is required.");
        }
        return errors;
    }

    public List<String> validateJobPosting(JobPosting jobPosting) {
        List<String> errors = new ArrayList<>();
        if (FileUtil.isBlank(jobPosting.getModuleCode())) {
            errors.add("Module code is required.");
        }
        if (FileUtil.isBlank(jobPosting.getModuleTitle())) {
            errors.add("Module title is required.");
        }
        if (jobPosting.getHours() <= 0) {
            errors.add("Hours must be greater than 0.");
        }
        if (jobPosting.getApplicationDeadline() == null) {
            errors.add("Application deadline is required.");
        }
        if (jobPosting.getRequiredSkills() == null || jobPosting.getRequiredSkills().isEmpty()) {
            errors.add("At least one required skill is needed.");
        }
        return errors;
    }

    public List<String> parseSkills(String commaSeparatedSkills) {
        if (FileUtil.isBlank(commaSeparatedSkills)) {
            return new ArrayList<>();
        }
        return Arrays.stream(commaSeparatedSkills.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
