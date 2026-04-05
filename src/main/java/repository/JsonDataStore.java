package repository;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.JobPosting;
import model.JobStatus;
import model.Role;
import model.SystemConfig;
import model.User;
import util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared JSON persistence helper for collections and config objects.
 */
public class JsonDataStore {
    public <T> List<T> readList(Path path, Class<T> itemClass) {
        initializeListFile(path);
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = JsonUtil.parse(json);
            if (!(parsed instanceof List<?> items)) {
                throw new IllegalStateException("Expected JSON array in " + path);
            }
            List<T> results = new ArrayList<>();
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> rawMap)) {
                    throw new IllegalStateException("Expected object entries in " + path);
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) rawMap;
                results.add(convertMap(itemClass, map));
            }
            return results;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read data from " + path, e);
        }
    }

    public <T> void writeList(Path path, List<T> values) {
        ensureParent(path);
        try {
            List<Object> serialized = new ArrayList<>();
            for (T value : values) {
                serialized.add(serialize(value));
            }
            Files.writeString(path, JsonUtil.toPrettyJson(serialized), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write data to " + path, e);
        }
    }

    public <T> T readObject(Path path, Class<T> clazz, T defaultValue) {
        initializeObjectFile(path, defaultValue);
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = JsonUtil.parse(json);
            if (!(parsed instanceof Map<?, ?> rawMap)) {
                throw new IllegalStateException("Expected JSON object in " + path);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;
            return convertMap(clazz, map);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config from " + path, e);
        }
    }

    public <T> void writeObject(Path path, T value) {
        ensureParent(path);
        try {
            Files.writeString(path, JsonUtil.toPrettyJson(serialize(value)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write config to " + path, e);
        }
    }

    private void initializeListFile(Path path) {
        ensureParent(path);
        if (!Files.exists(path)) {
            writeList(path, new ArrayList<>());
        }
    }

    private <T> void initializeObjectFile(Path path, T defaultValue) {
        ensureParent(path);
        if (!Files.exists(path)) {
            writeObject(path, defaultValue);
        }
    }

    private void ensureParent(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertMap(Class<T> clazz, Map<String, Object> map) {
        if (clazz == User.class) {
            User user = new User();
            user.setUserId(stringValue(map.get("userId")));
            user.setUsername(stringValue(map.get("username")));
            user.setPassword(stringValue(map.get("password")));
            user.setRole(enumValue(Role.class, map.get("role")));
            return (T) user;
        }
        if (clazz == ApplicantProfile.class) {
            ApplicantProfile profile = new ApplicantProfile();
            profile.setApplicantId(stringValue(map.get("applicantId")));
            profile.setUserId(stringValue(map.get("userId")));
            profile.setName(stringValue(map.get("name")));
            profile.setEmail(stringValue(map.get("email")));
            profile.setPhone(stringValue(map.get("phone")));
            profile.setSkills(stringList(map.get("skills")));
            profile.setAvailability(stringValue(map.get("availability")));
            profile.setExperienceSummary(stringValue(map.get("experienceSummary")));
            profile.setPreferredDuties(stringValue(map.get("preferredDuties")));
            profile.setCvPath(stringValue(map.get("cvPath")));
            return (T) profile;
        }
        if (clazz == JobPosting.class) {
            JobPosting job = new JobPosting();
            job.setJobId(stringValue(map.get("jobId")));
            job.setModuleCode(stringValue(map.get("moduleCode")));
            job.setModuleTitle(stringValue(map.get("moduleTitle")));
            job.setDuties(stringValue(map.get("duties")));
            job.setHours(intValue(map.get("hours")));
            job.setRequiredSkills(stringList(map.get("requiredSkills")));
            String deadline = stringValue(map.get("applicationDeadline"));
            job.setApplicationDeadline(deadline == null || deadline.isBlank() ? null : LocalDate.parse(deadline));
            job.setStatus(enumValue(JobStatus.class, map.get("status")));
            job.setPostedBy(stringValue(map.get("postedBy")));
            return (T) job;
        }
        if (clazz == ApplicationRecord.class) {
            ApplicationRecord record = new ApplicationRecord();
            record.setApplicationId(stringValue(map.get("applicationId")));
            record.setApplicantId(stringValue(map.get("applicantId")));
            record.setJobId(stringValue(map.get("jobId")));
            String appliedAt = stringValue(map.get("appliedAt"));
            record.setAppliedAt(appliedAt == null || appliedAt.isBlank() ? null : LocalDateTime.parse(appliedAt));
            record.setStatus(enumValue(ApplicationStatus.class, map.get("status")));
            record.setReviewerNotes(stringValue(map.get("reviewerNotes")));
            record.setMatchScore(intValue(map.get("matchScore")));
            record.setMissingSkills(stringList(map.get("missingSkills")));
            return (T) record;
        }
        if (clazz == SystemConfig.class) {
            SystemConfig config = new SystemConfig();
            Object threshold = map.get("workloadThreshold");
            if (threshold != null) {
                config.setWorkloadThreshold(intValue(threshold));
            }
            return (T) config;
        }
        throw new IllegalArgumentException("Unsupported class: " + clazz.getName());
    }

    private Object serialize(Object value) {
        if (value instanceof User user) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId", user.getUserId());
            map.put("username", user.getUsername());
            map.put("password", user.getPassword());
            map.put("role", user.getRole() == null ? null : user.getRole().name());
            return map;
        }
        if (value instanceof ApplicantProfile profile) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("applicantId", profile.getApplicantId());
            map.put("userId", profile.getUserId());
            map.put("name", profile.getName());
            map.put("email", profile.getEmail());
            map.put("phone", profile.getPhone());
            map.put("skills", new ArrayList<>(profile.getSkills()));
            map.put("availability", profile.getAvailability());
            map.put("experienceSummary", profile.getExperienceSummary());
            map.put("preferredDuties", profile.getPreferredDuties());
            map.put("cvPath", profile.getCvPath());
            return map;
        }
        if (value instanceof JobPosting job) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("jobId", job.getJobId());
            map.put("moduleCode", job.getModuleCode());
            map.put("moduleTitle", job.getModuleTitle());
            map.put("duties", job.getDuties());
            map.put("hours", job.getHours());
            map.put("requiredSkills", new ArrayList<>(job.getRequiredSkills()));
            map.put("applicationDeadline", job.getApplicationDeadline() == null ? null : job.getApplicationDeadline().toString());
            map.put("status", job.getStatus() == null ? null : job.getStatus().name());
            map.put("postedBy", job.getPostedBy());
            return map;
        }
        if (value instanceof ApplicationRecord record) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("applicationId", record.getApplicationId());
            map.put("applicantId", record.getApplicantId());
            map.put("jobId", record.getJobId());
            map.put("appliedAt", record.getAppliedAt() == null ? null : record.getAppliedAt().toString());
            map.put("status", record.getStatus() == null ? null : record.getStatus().name());
            map.put("reviewerNotes", record.getReviewerNotes());
            map.put("matchScore", record.getMatchScore());
            map.put("missingSkills", new ArrayList<>(record.getMissingSkills()));
            return map;
        }
        if (value instanceof SystemConfig config) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("workloadThreshold", config.getWorkloadThreshold());
            return map;
        }
        throw new IllegalArgumentException("Unsupported value: " + value.getClass().getName());
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private List<String> stringList(Object value) {
        List<String> results = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                results.add(stringValue(item));
            }
        }
        return results;
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumClass, Object value) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumClass, String.valueOf(value));
    }
}
