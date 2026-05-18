package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonDataStore;
import repository.NotificationRepository;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldCreateAndMarkNotificationsRead() {
        NotificationRepository repository = new NotificationRepository(new JsonDataStore(), tempDir.resolve("notifications.json"));
        NotificationService service = new NotificationService(repository);

        service.notifyUser("u1", "Application submitted.");
        assertEquals(1, service.getNotificationsForUser("u1").size());
        assertTrue(service.getNotificationsForUser("u1").stream().noneMatch(notification -> notification.isRead()));
        assertTrue(service.hasUnreadNotifications("u1"));

        service.markAllRead("u1");
        assertTrue(service.getNotificationsForUser("u1").stream().allMatch(notification -> notification.isRead()));
        assertFalse(service.hasUnreadNotifications("u1"));

        NotificationService reloadedService = new NotificationService(
                new NotificationRepository(new JsonDataStore(), tempDir.resolve("notifications.json"))
        );
        assertFalse(reloadedService.hasUnreadNotifications("u1"));
    }
}
