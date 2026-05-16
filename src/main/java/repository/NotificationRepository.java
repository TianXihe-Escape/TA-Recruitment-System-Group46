package repository;

import model.NotificationRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for in-system notifications.
 */
public class NotificationRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public NotificationRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public List<NotificationRecord> findAll() {
        return dataStore.readList(filePath, NotificationRecord.class);
    }

    public List<NotificationRecord> findByUserId(String userId) {
        return findAll().stream()
                .filter(notification -> userId != null && userId.equals(notification.getUserId()))
                .collect(Collectors.toList());
    }

    public void saveAll(List<NotificationRecord> notifications) {
        dataStore.writeList(filePath, notifications);
    }
}
