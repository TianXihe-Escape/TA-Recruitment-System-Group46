package repository;

import model.ApplicantProfile;
import model.ApplicationRecord;
import model.ApplicationStatus;
import model.AllocationRecord;
import model.JobCategory;
import model.JobPosting;
import model.JobStatus;
import model.MessageRecord;
import model.NotificationRecord;
import model.Role;
import model.StatusHistoryEntry;
import model.SystemConfig;
import model.User;
import util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level JSON persistence helper shared by all repositories.
 * This class centralizes file initialization, JSON parsing, object mapping, and
 * serialization so repository classes can stay small and domain-focused.
 */
public class JsonDataStore {
    /**
     * Reads a JSON array file and converts each object entry into the requested type.
     */
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
            throw new IllegalStateException("Could not read JSON data file " + path + ".", e);
        } catch (RuntimeException e) {
            throw damagedFileException(path, e);
        }
    }

    /**
     * Writes a list of domain objects as a pretty-printed JSON array.
     */
    public <T> void writeList(Path path, List<T> values) {
        ensureParent(path);
        try {
            List<Object> serialized = new ArrayList<>();
            for (T value : values) {
                serialized.add(serialize(value));
            }
            writeStringAtomically(path, JsonUtil.toPrettyJson(serialized));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write data to " + path, e);
        }
    }

    /**
     * Reads a single JSON object file, creating it with a default value if missing.
     */
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
            throw new IllegalStateException("Could not read JSON data file " + path + ".", e);
        } catch (RuntimeException e) {
            throw damagedFileException(path, e);
        }
    }

    /**
     * Writes a single object as a pretty-printed JSON object.
     */
    public <T> void writeObject(Path path, T value) {
        ensureParent(path);
        try {
            writeStringAtomically(path, JsonUtil.toPrettyJson(serialize(value)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write config to " + path, e);
        }
    }

    /**
     * Ensures an array-backed JSON file exists before read operations.
     */
    private void initializeListFile(Path path) {
        ensureParent(path);
        if (!Files.exists(path)) {
            writeList(path, new ArrayList<>());
        }
    }

    /**
     * Ensures an object-backed JSON file exists before read operations.
     */
    private <T> void initializeObjectFile(Path path, T defaultValue) {
        ensureParent(path);
        if (!Files.exists(path)) {
            writeObject(path, defaultValue);
        }
    }

    /**
     * Creates the parent directory tree for a data file if needed.
     */
    private void ensureParent(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize " + path, e);
        }
    }

    private void writeStringAtomically(Path path, String content) throws IOException {
        ensureParent(path);
        Path temp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            // Write-then-move avoids leaving a half-written JSON file behind if saving is interrupted.
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private IllegalStateException damagedFileException(Path path, RuntimeException cause) {
        return new IllegalStateException(
                "The JSON data file appears to be damaged: " + path
                        + ". Restore sample data from the application or delete the damaged file so it can be recreated.",
                cause
        );
    }

    @SuppressWarnings("unchecked")
    /**
     * Converts a parsed JSON object map into one of the supported domain models.
     */
    private <T> T convertMap(Class<T> clazz, Map<String, Object> map) {
        // This central mapping keeps the on-disk JSON schema explicit and easy to evolve.
        if (clazz == User.class) {
            User user = new User();
            user.setUserId(stringValue(map.get("userId")));
            user.setName(stringValue(map.get("name")));
            user.setUsername(stringValue(map.get("username")));
            user.setPassword(stringValue(map.get("password")));
            user.setRole(enumValue(Role.class, map.get("role")));
            user.setManagedModuleCodes(stringList(map.get("managedModuleCodes")));
            return (T) user;
        }
        if (clazz == ApplicantProfile.class) {
            ApplicantProfile profile = new ApplicantProfile();
            profile.setApplicantId(stringValue(map.get("applicantId")));
            profile.setUserId(stringValue(map.get("userId")));
            profile.setName(stringValue(map.get("name")));
            profile.setEmail(stringValue(map.get("email")));
            profile.setPhone(stringValue(map.get("phone")));
            profile.setProgramme(stringValue(map.get("programme")));
            profile.setYearOfStudy(stringValue(map.get("yearOfStudy")));
            profile.setSkills(stringList(map.get("skills")));
            profile.setAvailability(stringValue(map.get("availability")));
            profile.setExperienceSummary(stringValue(map.get("experienceSummary")));
            profile.setPreferredDuties(stringValue(map.get("preferredDuties")));
            profile.setCvPath(stringValue(map.get("cvPath")));
            List<String> supportingDocumentPaths = stringList(map.get("supportingDocumentPaths"));
            if (supportingDocumentPaths.isEmpty()) {
                // Older saved data used a single path field, so keep backward compatibility here.
                profile.setSupportingDocumentPath(stringValue(map.get("supportingDocumentPath")));
            } else {
                profile.setSupportingDocumentPaths(supportingDocumentPaths);
            }
            profile.setFavoriteJobIds(stringList(map.get("favoriteJobIds")));
            return (T) profile;
        }
        if (clazz == JobPosting.class) {
            JobPosting job = new JobPosting();
            job.setJobId(stringValue(map.get("jobId")));
            job.setModuleCode(stringValue(map.get("moduleCode")));
            job.setModuleTitle(stringValue(map.get("moduleTitle")));
            job.setCategory(enumValueOrDefault(JobCategory.class, map.get("category"), JobCategory.MODULE_TA));
            job.setSemester(stringValue(map.get("semester")));
            job.setDuties(stringValue(map.get("duties")));
            job.setHours(intValue(map.get("hours")));
            job.setJobType(stringValue(map.get("jobType")));
            job.setStartDate(stringValue(map.get("startDate")));
            job.setEndDate(stringValue(map.get("endDate")));
            job.setSchedule(stringValue(map.get("schedule")));
            job.setLocation(stringValue(map.get("location")));
            job.setWorkloadType(stringValue(map.get("workloadType")));
            Object requiredTaCount = map.get("requiredTaCount");
            job.setRequiredTaCount(requiredTaCount == null ? 1 : intValue(requiredTaCount));
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
            String lastUpdatedAt = stringValue(map.get("lastUpdatedAt"));
            record.setLastUpdatedAt(lastUpdatedAt == null || lastUpdatedAt.isBlank() ? null : LocalDateTime.parse(lastUpdatedAt));
            String decisionAt = stringValue(map.get("decisionAt"));
            record.setDecisionAt(decisionAt == null || decisionAt.isBlank() ? null : LocalDateTime.parse(decisionAt));
            record.setStatus(enumValue(ApplicationStatus.class, map.get("status")));
            record.setReviewerNotes(stringValue(map.get("reviewerNotes")));
            record.setMatchScore(intValue(map.get("matchScore")));
            record.setMissingSkills(stringList(map.get("missingSkills")));
            // History may be absent in legacy files, so treat it as optional during reads.
            record.setStatusHistory(statusHistoryList(map.get("statusHistory")));
            return (T) record;
        }
        if (clazz == NotificationRecord.class) {
            NotificationRecord notification = new NotificationRecord();
            notification.setNotificationId(stringValue(map.get("notificationId")));
            notification.setUserId(stringValue(map.get("userId")));
            notification.setMessage(stringValue(map.get("message")));
            String createdAt = stringValue(map.get("createdAt"));
            notification.setCreatedAt(createdAt == null || createdAt.isBlank() ? null : LocalDateTime.parse(createdAt));
            notification.setRead(booleanValue(map.get("read")));
            return (T) notification;
        }
        if (clazz == MessageRecord.class) {
            MessageRecord message = new MessageRecord();
            message.setMessageId(stringValue(map.get("messageId")));
            message.setJobId(stringValue(map.get("jobId")));
            message.setApplicationId(stringValue(map.get("applicationId")));
            message.setSenderUserId(stringValue(map.get("senderUserId")));
            message.setRecipientUserId(stringValue(map.get("recipientUserId")));
            message.setBody(stringValue(map.get("body")));
            String createdAt = stringValue(map.get("createdAt"));
            message.setCreatedAt(createdAt == null || createdAt.isBlank() ? null : LocalDateTime.parse(createdAt));
            message.setRead(booleanValue(map.get("read")));
            return (T) message;
        }
        if (clazz == AllocationRecord.class) {
            AllocationRecord allocation = new AllocationRecord();
            allocation.setAllocationId(stringValue(map.get("allocationId")));
            allocation.setApplicationId(stringValue(map.get("applicationId")));
            allocation.setApplicantId(stringValue(map.get("applicantId")));
            allocation.setJobId(stringValue(map.get("jobId")));
            allocation.setAllocatedByUserId(stringValue(map.get("allocatedByUserId")));
            String allocatedAt = stringValue(map.get("allocatedAt"));
            allocation.setAllocatedAt(allocatedAt == null || allocatedAt.isBlank() ? null : LocalDateTime.parse(allocatedAt));
            allocation.setActive(map.get("active") == null || booleanValue(map.get("active")));
            return (T) allocation;
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

    /**
     * Converts a supported domain model into an ordered map structure for JSON output.
     */
    private Object serialize(Object value) {
        // Use LinkedHashMap so exported JSON keeps a stable field order for easier manual inspection.
        if (value instanceof User user) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId", user.getUserId());
            map.put("name", user.getName());
            map.put("username", user.getUsername());
            map.put("password", user.getPassword());
            map.put("role", user.getRole() == null ? null : user.getRole().name());
            map.put("managedModuleCodes", new ArrayList<>(user.getManagedModuleCodes()));
            return map;
        }
        if (value instanceof ApplicantProfile profile) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("applicantId", profile.getApplicantId());
            map.put("userId", profile.getUserId());
            map.put("name", profile.getName());
            map.put("email", profile.getEmail());
            map.put("phone", profile.getPhone());
            map.put("programme", profile.getProgramme());
            map.put("yearOfStudy", profile.getYearOfStudy());
            map.put("skills", new ArrayList<>(profile.getSkills()));
            map.put("availability", profile.getAvailability());
            map.put("experienceSummary", profile.getExperienceSummary());
            map.put("preferredDuties", profile.getPreferredDuties());
            map.put("cvPath", profile.getCvPath());
            map.put("supportingDocumentPath", profile.getSupportingDocumentPath());
            map.put("supportingDocumentPaths", new ArrayList<>(profile.getSupportingDocumentPaths()));
            map.put("favoriteJobIds", new ArrayList<>(profile.getFavoriteJobIds()));
            return map;
        }
        if (value instanceof JobPosting job) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("jobId", job.getJobId());
            map.put("moduleCode", job.getModuleCode());
            map.put("moduleTitle", job.getModuleTitle());
            map.put("category", job.getCategory() == null ? null : job.getCategory().name());
            map.put("semester", job.getSemester());
            map.put("duties", job.getDuties());
            map.put("hours", job.getHours());
            map.put("jobType", job.getJobType());
            map.put("startDate", job.getStartDate());
            map.put("endDate", job.getEndDate());
            map.put("schedule", job.getSchedule());
            map.put("location", job.getLocation());
            map.put("workloadType", job.getWorkloadType());
            map.put("requiredTaCount", job.getRequiredTaCount());
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
            map.put("lastUpdatedAt", record.getLastUpdatedAt() == null ? null : record.getLastUpdatedAt().toString());
            map.put("decisionAt", record.getDecisionAt() == null ? null : record.getDecisionAt().toString());
            map.put("status", record.getStatus() == null ? null : record.getStatus().name());
            map.put("reviewerNotes", record.getReviewerNotes());
            map.put("matchScore", record.getMatchScore());
            map.put("missingSkills", new ArrayList<>(record.getMissingSkills()));
            List<Object> history = new ArrayList<>();
            for (StatusHistoryEntry entry : record.getStatusHistory()) {
                history.add(serializeStatusHistoryEntry(entry));
            }
            map.put("statusHistory", history);
            return map;
        }
        if (value instanceof NotificationRecord notification) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("notificationId", notification.getNotificationId());
            map.put("userId", notification.getUserId());
            map.put("message", notification.getMessage());
            map.put("createdAt", notification.getCreatedAt() == null ? null : notification.getCreatedAt().toString());
            map.put("read", notification.isRead());
            return map;
        }
        if (value instanceof MessageRecord message) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("messageId", message.getMessageId());
            map.put("jobId", message.getJobId());
            map.put("applicationId", message.getApplicationId());
            map.put("senderUserId", message.getSenderUserId());
            map.put("recipientUserId", message.getRecipientUserId());
            map.put("body", message.getBody());
            map.put("createdAt", message.getCreatedAt() == null ? null : message.getCreatedAt().toString());
            map.put("read", message.isRead());
            return map;
        }
        if (value instanceof AllocationRecord allocation) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("allocationId", allocation.getAllocationId());
            map.put("applicationId", allocation.getApplicationId());
            map.put("applicantId", allocation.getApplicantId());
            map.put("jobId", allocation.getJobId());
            map.put("allocatedByUserId", allocation.getAllocatedByUserId());
            map.put("allocatedAt", allocation.getAllocatedAt() == null ? null : allocation.getAllocatedAt().toString());
            map.put("active", allocation.isActive());
            return map;
        }
        if (value instanceof SystemConfig config) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("workloadThreshold", config.getWorkloadThreshold());
            return map;
        }
        throw new IllegalArgumentException("Unsupported value: " + value.getClass().getName());
    }

    /**
     * Converts any scalar JSON value into a string, preserving null.
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Converts numeric JSON values into integers, accepting both JSON numbers and strings.
     */
    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * Converts a parsed JSON array into a list of strings.
     */
    private List<String> stringList(Object value) {
        List<String> results = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                results.add(stringValue(item));
            }
        }
        return results;
    }

    private List<StatusHistoryEntry> statusHistoryList(Object value) {
        List<StatusHistoryEntry> results = new ArrayList<>();
        if (!(value instanceof List<?> items)) {
            return results;
        }
        // Skip malformed entries instead of failing the whole application record load.
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;
            StatusHistoryEntry entry = new StatusHistoryEntry();
            entry.setStatus(enumValueOrDefault(ApplicationStatus.class, map.get("status"), ApplicationStatus.SUBMITTED));
            String changedAt = stringValue(map.get("changedAt"));
            entry.setChangedAt(changedAt == null || changedAt.isBlank() ? null : LocalDateTime.parse(changedAt));
            entry.setActorUserId(stringValue(map.get("actorUserId")));
            entry.setNote(stringValue(map.get("note")));
            results.add(entry);
        }
        return results;
    }

    private Map<String, Object> serializeStatusHistoryEntry(StatusHistoryEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", entry.getStatus() == null ? null : entry.getStatus().name());
        map.put("changedAt", entry.getChangedAt() == null ? null : entry.getChangedAt().toString());
        map.put("actorUserId", entry.getActorUserId());
        map.put("note", entry.getNote());
        return map;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Converts a stored enum name back into the target enum constant.
     */
    private <E extends Enum<E>> E enumValue(Class<E> enumClass, Object value) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumClass, String.valueOf(value));
    }

    private <E extends Enum<E>> E enumValueOrDefault(Class<E> enumClass, Object value, E defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        // Defaults let newer code read older JSON files that did not yet store the enum field.
        return Enum.valueOf(enumClass, String.valueOf(value));
    }
}
