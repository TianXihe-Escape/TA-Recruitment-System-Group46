package ui;

import model.JobPosting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formatting helpers for user-facing UI text.
 *
 * This utility centralises small display-only transformations so tables,
 * cards, dialogs, and review panes all present dates, placeholders, and job
 * details in the same way across the Swing interface.
 */
public final class UiFormat {
    /**
     * Standard date-time format used in notifications, messages, and audit views.
     */
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /**
     * Standard date format used for deadlines and date-only labels.
     */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Utility class; callers use the static formatting helpers only.
     */
    private UiFormat() {
        // Utility class.
    }

    /**
     * Formats a date-time value for UI display.
     *
     * Null values are converted into a dash so callers can render missing data
     * directly without repeating null-check logic in every screen.
     */
    public static String dateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    /**
     * Formats a date-only value for UI display.
     */
    public static String date(LocalDate value) {
        return value == null ? "-" : DATE.format(value);
    }

    /**
     * Replaces null or blank strings with a stable placeholder.
     *
     * A shared placeholder strategy helps tables and cards stay visually aligned
     * when optional fields are missing from JSON-backed data.
     */
    public static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * Formats one job's workload using the correct unit label.
     *
     * The wording depends on whether the posting uses a total one-off workload
     * or a weekly recurring workload.
     */
    public static String workload(JobPosting job) {
        if (job == null) {
            return "-";
        }
        String suffix = job.isTotalWorkload() ? "h total" : "h/week";
        return job.getHours() + " " + suffix;
    }

    /**
     * Formats a job's active period from optional start/end date strings.
     *
     * The method tolerates partial data so jobs with only one known boundary
     * still render cleanly instead of exposing raw blank values.
     */
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

    /**
     * Combines schedule and location into one compact table-friendly string.
     *
     * Missing values are collapsed intelligently so users never see separators
     * like "|" unless both pieces of information actually exist.
     */
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
