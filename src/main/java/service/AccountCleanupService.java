package service;

import model.AllocationRecord;
import model.MessageRecord;
import model.NotificationRecord;
import repository.AllocationRepository;
import repository.MessageRepository;
import repository.NotificationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Removes secondary records that would otherwise point at deleted accounts,
 * applications, or jobs.
 */
public class AccountCleanupService {
    private final AllocationRepository allocationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;

    public AccountCleanupService(AllocationRepository allocationRepository,
                                 MessageRepository messageRepository,
                                 NotificationRepository notificationRepository) {
        this.allocationRepository = allocationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
    }

    public void cleanupDeletedApplicant(String applicantId, String userId, Set<String> deletedApplicationIds) {
        cleanupAllocations(applicantId, Set.of(), deletedApplicationIds);
        cleanupMessages(userId, Set.of(), deletedApplicationIds);
        cleanupNotifications(userId);
    }

    public void cleanupDeletedModuleOrganiser(String userId, Set<String> deletedJobIds, Set<String> deletedApplicationIds) {
        cleanupAllocations(null, deletedJobIds, deletedApplicationIds);
        cleanupMessages(userId, deletedJobIds, deletedApplicationIds);
        cleanupNotifications(userId);
    }

    private void cleanupAllocations(String applicantId, Set<String> deletedJobIds, Set<String> deletedApplicationIds) {
        List<AllocationRecord> allocations = new ArrayList<>(allocationRepository.findAll());
        boolean removed = allocations.removeIf(allocation ->
                (applicantId != null && applicantId.equals(allocation.getApplicantId()))
                        || deletedJobIds.contains(allocation.getJobId())
                        || deletedApplicationIds.contains(allocation.getApplicationId()));
        if (removed) {
            allocationRepository.saveAll(allocations);
        }
    }

    private void cleanupMessages(String userId, Set<String> deletedJobIds, Set<String> deletedApplicationIds) {
        List<MessageRecord> messages = new ArrayList<>(messageRepository.findAll());
        boolean removed = messages.removeIf(message ->
                (userId != null && (userId.equals(message.getSenderUserId()) || userId.equals(message.getRecipientUserId())))
                        || deletedJobIds.contains(message.getJobId())
                        || deletedApplicationIds.contains(message.getApplicationId()));
        if (removed) {
            messageRepository.saveAll(messages);
        }
    }

    private void cleanupNotifications(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        List<NotificationRecord> notifications = new ArrayList<>(notificationRepository.findAll());
        boolean removed = notifications.removeIf(notification -> userId.equals(notification.getUserId()));
        if (removed) {
            notificationRepository.saveAll(notifications);
        }
    }
}
