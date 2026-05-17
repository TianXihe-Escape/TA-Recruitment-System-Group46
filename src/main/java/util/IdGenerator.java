package util;

import model.ApplicantProfile;
import model.Role;
import model.User;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates readable prefixed identifiers for persisted records.
 */
public final class IdGenerator {
    private IdGenerator() {
    }

    public static String nextUserId(Role role, List<User> users) {
        String prefix = "user-" + role.name().toLowerCase() + "-";
        return prefix + twoDigitNumber(nextIndex(
                users.stream()
                        .map(User::getUserId)
                        .toList(),
                prefix
        ));
    }

    public static String nextApplicantId(List<ApplicantProfile> profiles) {
        String prefix = "applicant-";
        return prefix + twoDigitNumber(nextIndex(
                profiles.stream()
                        .map(ApplicantProfile::getApplicantId)
                        .toList(),
                prefix
        ));
    }

    public static String nextJobId(List<String> jobIds) {
        String prefix = "job-";
        return prefix + twoDigitNumber(nextIndex(jobIds, prefix));
    }

    public static String nextJobId(String moduleCode, List<String> jobIds) {
        String normalizedModule = moduleCode == null
                ? ""
                : moduleCode.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        if (normalizedModule.isBlank()) {
            return nextJobId(jobIds);
        }
        String prefix = "job-" + normalizedModule + "-";
        return prefix + twoDigitNumber(nextIndex(jobIds, prefix));
    }

    private static int nextIndex(List<String> ids, String prefix) {
        int max = 0;
        Pattern exactNumberPattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            Matcher matcher = exactNumberPattern.matcher(id);
            if (matcher.matches()) {
                max = Math.max(max, Integer.parseInt(matcher.group(1)));
            }
        }
        return max + 1;
    }

    private static String twoDigitNumber(int value) {
        return String.format("%02d", value);
    }
}
