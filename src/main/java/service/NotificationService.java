package service;

import model.NotificationRecord;
import repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates and reads in-system notification records.
 */
public class NotificationService {
    private static final Pattern NOTIFICATION_ID_PATTERN = Pattern.compile("^note-(\\d+)$");

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notifyUser(String userId, String message) {
        if (userId == null || userId.isBlank() || message == null || message.isBlank()) {
            return;
        }
        List<NotificationRecord> notifications = new ArrayList<>(notificationRepository.findAll());
        NotificationRecord notification = new NotificationRecord();
        notification.setNotificationId(nextNotificationId(notifications));
        notification.setUserId(userId);
        notification.setMessage(message.trim());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notifications.add(notification);
        notificationRepository.saveAll(notifications);
    }

    public List<NotificationRecord> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(NotificationRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public List<NotificationRecord> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .sorted(Comparator.comparing(NotificationRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public void markAllRead(String userId) {
        List<NotificationRecord> notifications = new ArrayList<>(notificationRepository.findAll());
        boolean updated = false;
        for (NotificationRecord notification : notifications) {
            if (userId != null && userId.equals(notification.getUserId()) && !notification.isRead()) {
                notification.setRead(true);
                updated = true;
            }
        }
        if (updated) {
            notificationRepository.saveAll(notifications);
        }
    }

    private String nextNotificationId(List<NotificationRecord> notifications) {
        int next = notifications.stream()
                .map(NotificationRecord::getNotificationId)
                .mapToInt(this::extractSequence)
                .max()
                .orElse(0) + 1;
        return String.format("note-%02d", next);
    }

    private int extractSequence(String notificationId) {
        if (notificationId == null) {
            return 0;
        }
        Matcher matcher = NOTIFICATION_ID_PATTERN.matcher(notificationId.trim());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
