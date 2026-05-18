package service;

import model.AllocationRecord;
import model.MessageRecord;
import model.NotificationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.AllocationRepository;
import repository.JsonDataStore;
import repository.MessageRepository;
import repository.NotificationRepository;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountCleanupServiceTest {
    @TempDir
    Path tempDir;

    private AllocationRepository allocationRepository;
    private MessageRepository messageRepository;
    private NotificationRepository notificationRepository;
    private AccountCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        JsonDataStore dataStore = new JsonDataStore();
        allocationRepository = new AllocationRepository(dataStore, tempDir.resolve("allocations.json"));
        messageRepository = new MessageRepository(dataStore, tempDir.resolve("messages.json"));
        notificationRepository = new NotificationRepository(dataStore, tempDir.resolve("notifications.json"));
        cleanupService = new AccountCleanupService(allocationRepository, messageRepository, notificationRepository);
    }

    @Test
    void shouldRemoveSecondaryRecordsForDeletedApplicant() {
        allocationRepository.saveAll(List.of(
                allocation("alloc-01", "apply-ta", "applicant-ta", "job-01"),
                allocation("alloc-02", "apply-other", "applicant-other", "job-02")
        ));
        messageRepository.saveAll(List.of(
                message("msg-01", "job-01", "apply-ta", "user-ta", "user-mo"),
                message("msg-02", "job-02", "apply-other", "user-other", "user-mo")
        ));
        notificationRepository.saveAll(List.of(
                notification("note-01", "user-ta"),
                notification("note-02", "user-other")
        ));

        cleanupService.cleanupDeletedApplicant("applicant-ta", "user-ta", Set.of("apply-ta"));

        assertEquals(List.of("alloc-02"), allocationRepository.findAll().stream().map(AllocationRecord::getAllocationId).toList());
        assertEquals(List.of("msg-02"), messageRepository.findAll().stream().map(MessageRecord::getMessageId).toList());
        assertEquals(List.of("note-02"), notificationRepository.findAll().stream().map(NotificationRecord::getNotificationId).toList());
    }

    @Test
    void shouldRemoveSecondaryRecordsForDeletedModuleOrganiserJobs() {
        allocationRepository.saveAll(List.of(
                allocation("alloc-01", "apply-deleted", "applicant-ta", "job-deleted"),
                allocation("alloc-02", "apply-other", "applicant-other", "job-other")
        ));
        messageRepository.saveAll(List.of(
                message("msg-01", "job-deleted", "apply-deleted", "user-mo", "user-ta"),
                message("msg-02", "job-other", "apply-other", "user-other", "user-ta")
        ));
        notificationRepository.saveAll(List.of(
                notification("note-01", "user-mo"),
                notification("note-02", "user-ta")
        ));

        cleanupService.cleanupDeletedModuleOrganiser("user-mo", Set.of("job-deleted"), Set.of("apply-deleted"));

        assertTrue(allocationRepository.findAll().stream().noneMatch(allocation -> "job-deleted".equals(allocation.getJobId())));
        assertTrue(messageRepository.findAll().stream().noneMatch(message -> "user-mo".equals(message.getSenderUserId())));
        assertEquals(List.of("note-02"), notificationRepository.findAll().stream().map(NotificationRecord::getNotificationId).toList());
    }

    private AllocationRecord allocation(String allocationId, String applicationId, String applicantId, String jobId) {
        AllocationRecord allocation = new AllocationRecord();
        allocation.setAllocationId(allocationId);
        allocation.setApplicationId(applicationId);
        allocation.setApplicantId(applicantId);
        allocation.setJobId(jobId);
        allocation.setAllocatedByUserId("admin");
        allocation.setAllocatedAt(LocalDateTime.now());
        allocation.setActive(true);
        return allocation;
    }

    private MessageRecord message(String messageId, String jobId, String applicationId, String senderUserId, String recipientUserId) {
        MessageRecord message = new MessageRecord();
        message.setMessageId(messageId);
        message.setJobId(jobId);
        message.setApplicationId(applicationId);
        message.setSenderUserId(senderUserId);
        message.setRecipientUserId(recipientUserId);
        message.setBody("Message body");
        message.setCreatedAt(LocalDateTime.now());
        message.setRead(false);
        return message;
    }

    private NotificationRecord notification(String notificationId, String userId) {
        NotificationRecord notification = new NotificationRecord();
        notification.setNotificationId(notificationId);
        notification.setUserId(userId);
        notification.setMessage("Notification body");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        return notification;
    }
}
