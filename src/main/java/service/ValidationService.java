package service;

import model.JobPosting;
import util.FileUtil;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Central place for validating user input and forms.
 */
public class ValidationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}][\\p{L}\\p{M} .\\u00B7'\\-]{0,49}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+?\\d{7,15}|1[3-9]\\d{9})$");
    private static final int MAX_SKILLS = 10;
    private static final int MAX_SKILL_LENGTH = 30;
    private static final int MAX_MODULE_CODE_LENGTH = 20;
    private static final int MAX_MODULE_TITLE_LENGTH = 80;
    private static final int MAX_DUTIES_LENGTH = 300;
    private static final int MAX_REQUIRED_TA_COUNT = 20;
    private static final List<String> ALLOWED_CV_EXTENSIONS = List.of("pdf", "doc", "docx", "rtf", "txt");

    public List<String> validateRegistration(String username, String password, String confirmPassword) {
        List<String> errors = new ArrayList<>();
        String normalizedUsername = normalizeEmail(username);
        if (FileUtil.isBlank(normalizedUsername)) {
            errors.add("Email/username is required.");
        } else if (!EMAIL_PATTERN.matcher(normalizedUsername).matches()) {
            errors.add("Please enter a valid email address.");
        }
        if (FileUtil.isBlank(password)) {
            errors.add("Password is required.");
        } else if (password.length() < 4) {
            errors.add("Password must be at least 4 characters.");
        } else if (password.length() > 40) {
            errors.add("Password must be 40 characters or fewer.");
        }
        if (!String.valueOf(password).equals(confirmPassword)) {
            errors.add("Passwords do not match.");
        }
        return errors;
    }

    public List<String> validateLogin(String username, String password) {
        List<String> errors = new ArrayList<>();
        String normalizedUsername = normalizeEmail(username);
        if (FileUtil.isBlank(normalizedUsername)) {
            errors.add("Username is required.");
        } else if (!EMAIL_PATTERN.matcher(normalizedUsername).matches()) {
            errors.add("Please enter a valid email address.");
        }
        if (FileUtil.isBlank(password)) {
            errors.add("Password is required.");
        }
        return errors;
    }

    public List<String> validateApplicantProfile(String name, String email, String phone) {
        List<String> errors = new ArrayList<>();
        String normalizedName = normalizePersonName(name);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);

        errors.addAll(validatePersonName(normalizedName));
        if (FileUtil.isBlank(normalizedEmail) || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            errors.add("A valid email is required.");
        }
        if (FileUtil.isBlank(normalizedPhone)) {
            errors.add("Phone number is required.");
        } else if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            errors.add("Please enter a valid phone number.");
        }
        return errors;
    }

    public List<String> validatePersonName(String name) {
        List<String> errors = new ArrayList<>();
        String normalizedName = normalizePersonName(name);
        if (FileUtil.isBlank(normalizedName)) {
            errors.add("Name is required.");
        } else if (!NAME_PATTERN.matcher(normalizedName).matches()) {
            errors.add("Name can use letters, spaces, apostrophes, hyphens, and Chinese characters.");
        }
        return errors;
    }

    public List<String> validateManagedModuleCodes(List<String> managedModuleCodes) {
        List<String> errors = new ArrayList<>();
        List<String> normalizedCodes = managedModuleCodes == null
                ? List.of()
                : managedModuleCodes.stream()
                .map(this::normalizeModuleCode)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();

        if (normalizedCodes.isEmpty()) {
            errors.add("At least one managed module code is required for an MO account.");
            return errors;
        }

        if (normalizedCodes.stream().anyMatch(code -> code.length() > MAX_MODULE_CODE_LENGTH)) {
            errors.add("Each managed module code must be 20 characters or fewer.");
        }
        return errors;
    }

    public List<String> validateCvPath(String cvPath) {
        List<String> errors = new ArrayList<>();
        String normalizedPath = normalizeText(cvPath);
        if (FileUtil.isBlank(normalizedPath)) {
            return errors;
        }

        String fileName = Path.of(normalizedPath).getFileName() == null
                ? normalizedPath
                : Path.of(normalizedPath).getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0) {
            errors.add("CV file must use a common resume format: PDF, DOC, DOCX, RTF, or TXT.");
            return errors;
        }

        String extension = fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_CV_EXTENSIONS.contains(extension)) {
            errors.add("CV file must use a common resume format: PDF, DOC, DOCX, RTF, or TXT.");
        }
        return errors;
    }

    public List<String> validateJobPosting(JobPosting jobPosting) {
        List<String> errors = new ArrayList<>();
        String moduleCode = normalizeModuleCode(jobPosting.getModuleCode());
        String moduleTitle = normalizeText(jobPosting.getModuleTitle());
        String duties = normalizeMultilineText(jobPosting.getDuties());
        List<String> requiredSkills = parseSkills(String.join(", ", jobPosting.getRequiredSkills()));

        if (FileUtil.isBlank(moduleCode)) {
            errors.add("Module code is required.");
        } else if (moduleCode.length() > MAX_MODULE_CODE_LENGTH) {
            errors.add("Module code must be 20 characters or fewer.");
        }
        if (FileUtil.isBlank(moduleTitle)) {
            errors.add("Module title is required.");
        } else if (moduleTitle.length() > MAX_MODULE_TITLE_LENGTH) {
            errors.add("Module title must be 80 characters or fewer.");
        }
        if (jobPosting.getHours() <= 0) {
            errors.add("Hours must be greater than 0.");
        } else if (jobPosting.getHours() > 40) {
            errors.add("Hours must be 40 or fewer.");
        }
        if (jobPosting.getRequiredTaCount() <= 0) {
            errors.add("Required TA count must be greater than 0.");
        } else if (jobPosting.getRequiredTaCount() > MAX_REQUIRED_TA_COUNT) {
            errors.add("Required TA count must be 20 or fewer.");
        }
        if (jobPosting.getApplicationDeadline() == null) {
            errors.add("Application deadline is required.");
        } else if (jobPosting.getApplicationDeadline().isBefore(LocalDate.now())) {
            errors.add("Application deadline cannot be in the past.");
        }
        if (requiredSkills.isEmpty()) {
            errors.add("At least one required skill is needed.");
        } else if (requiredSkills.size() > MAX_SKILLS) {
            errors.add("Please keep required skills to 10 items or fewer.");
        }
        if (requiredSkills.stream().anyMatch(skill -> skill.length() > MAX_SKILL_LENGTH)) {
            errors.add("Each skill must be 30 characters or fewer.");
        }
        if (FileUtil.isBlank(duties)) {
            errors.add("Duties are required.");
        } else if (duties.length() > MAX_DUTIES_LENGTH) {
            errors.add("Duties must be 300 characters or fewer.");
        }
        return errors;
    }

    public List<String> parseSkills(String commaSeparatedSkills) {
        if (FileUtil.isBlank(commaSeparatedSkills)) {
            return new ArrayList<>();
        }
        Map<String, String> normalizedSkills = new LinkedHashMap<>();
        Arrays.stream(commaSeparatedSkills.split("[,;\\uFF0C\\uFF1B\\u3001\\n\\r]+"))
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .forEach(value -> normalizedSkills.putIfAbsent(value.toLowerCase(Locale.ROOT), value));
        return normalizedSkills.values().stream()
                .limit(MAX_SKILLS)
                .collect(Collectors.toList());
    }

    public String normalizeEmail(String email) {
        return normalizeText(email).toLowerCase(Locale.ROOT);
    }

    public String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[\\s()\\-]+", "").trim();
    }

    public String normalizePersonName(String name) {
        return normalizeText(name);
    }

    public String normalizeModuleCode(String moduleCode) {
        return normalizeText(moduleCode).toUpperCase(Locale.ROOT);
    }

    public String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public String normalizeMultilineText(String value) {
        if (value == null) {
            return "";
        }
        return Arrays.stream(value.replace("\r", "").split("\n", -1))
                .map(this::normalizeText)
                .collect(Collectors.joining("\n"))
                .trim();
    }
}
