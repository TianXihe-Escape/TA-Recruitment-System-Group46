package util;

import model.ApplicantProfile;
import model.Role;
import model.User;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates readable prefixed identifiers for persisted records.
 */
public final class IdGenerator {
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");

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

    private static int nextIndex(List<String> ids, String prefix) {
        int max = 0;
        for (String id : ids) {
            if (id == null || !id.startsWith(prefix)) {
                continue;
            }
            Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(id);
            if (matcher.find()) {
                max = Math.max(max, Integer.parseInt(matcher.group(1)));
            }
        }
        return max + 1;
    }

    private static String twoDigitNumber(int value) {
        return String.format("%02d", value);
    }
}
