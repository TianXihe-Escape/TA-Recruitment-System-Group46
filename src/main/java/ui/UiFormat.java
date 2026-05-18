package ui;

import model.JobPosting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formatting helpers for user-facing UI text.
 */
public final class UiFormat {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private UiFormat() {
        // Utility class.
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    public static String date(LocalDate value) {
        return value == null ? "-" : DATE.format(value);
    }

    public static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static String workload(JobPosting job) {
        if (job == null) {
            return "-";
        }
        String suffix = job.isTotalWorkload() ? "h total" : "h/week";
        return job.getHours() + " " + suffix;
    }

    public static String period(JobPosting job) {
        if (job == null) {
            return "-";
        }
        String start = valueOrDash(job.getStartDate());
        String end = valueOrDash(job.getEndDate());
        if ("-".equals(start) && "-".equals(end)) {
            return "-";
        }
        if (start.equals(end) || "-".equals(end)) {
            return start;
        }
        if ("-".equals(start)) {
            return end;
        }
        return start + " to " + end;
    }

    public static String scheduleLocation(JobPosting job) {
        if (job == null) {
            return "-";
        }
        String schedule = valueOrDash(job.getSchedule());
        String location = valueOrDash(job.getLocation());
        if ("-".equals(schedule)) {
            return location;
        }
        if ("-".equals(location)) {
            return schedule;
        }
        return schedule + " | " + location;
    }
}
