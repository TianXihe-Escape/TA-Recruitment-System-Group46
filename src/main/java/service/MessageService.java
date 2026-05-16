package service;

import model.MessageRecord;
import repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles lightweight TA/MO conversations stored in JSON.
 */
public class MessageService {
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("^msg-(\\d+)$");

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageRecord sendMessage(String jobId,
                                     String applicationId,
                                     String senderUserId,
                                     String recipientUserId,
                                     String body) {
        if (senderUserId == null || senderUserId.isBlank()) {
            throw new IllegalArgumentException("Message sender is required.");
        }
        if (recipientUserId == null || recipientUserId.isBlank()) {
            throw new IllegalArgumentException("Message recipient is required.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }

        List<MessageRecord> messages = new ArrayList<>(messageRepository.findAll());
        MessageRecord message = new MessageRecord();
        message.setMessageId(nextMessageId(messages));
        message.setJobId(blankToNull(jobId));
        message.setApplicationId(blankToNull(applicationId));
        message.setSenderUserId(senderUserId);
        message.setRecipientUserId(recipientUserId);
        message.setBody(body.trim());
        message.setCreatedAt(LocalDateTime.now());
        message.setRead(false);
        messages.add(message);
        messageRepository.saveAll(messages);
        return message;
    }

    public List<MessageRecord> getConversationForUser(String userId) {
        return messageRepository.findAll().stream()
                .filter(message -> userId != null
                        && (userId.equals(message.getSenderUserId()) || userId.equals(message.getRecipientUserId())))
                .sorted(Comparator.comparing(MessageRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public List<MessageRecord> getMessagesForJob(String jobId) {
        return messageRepository.findAll().stream()
                .filter(message -> jobId != null && jobId.equals(message.getJobId()))
                .sorted(Comparator.comparing(MessageRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public void markAllRead(String userId) {
        List<MessageRecord> messages = new ArrayList<>(messageRepository.findAll());
        boolean updated = false;
        for (MessageRecord message : messages) {
            if (userId != null && userId.equals(message.getRecipientUserId()) && !message.isRead()) {
                message.setRead(true);
                updated = true;
            }
        }
        if (updated) {
            messageRepository.saveAll(messages);
        }
    }

    public void markRead(String messageId, String userId) {
        List<MessageRecord> messages = new ArrayList<>(messageRepository.findAll());
        boolean updated = false;
        for (MessageRecord message : messages) {
            if (messageId != null
                    && messageId.equals(message.getMessageId())
                    && userId != null
                    && userId.equals(message.getRecipientUserId())
                    && !message.isRead()) {
                message.setRead(true);
                updated = true;
                break;
            }
        }
        if (updated) {
            messageRepository.saveAll(messages);
        }
    }

    private String nextMessageId(List<MessageRecord> messages) {
        int next = messages.stream()
                .map(MessageRecord::getMessageId)
                .mapToInt(this::extractSequence)
                .max()
                .orElse(0) + 1;
        return String.format("msg-%02d", next);
    }

    private int extractSequence(String messageId) {
        if (messageId == null) {
            return 0;
        }
        Matcher matcher = MESSAGE_ID_PATTERN.matcher(messageId.trim());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
