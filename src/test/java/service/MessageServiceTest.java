package service;

import model.MessageRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonDataStore;
import repository.MessageRepository;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldSendAndReadConversationMessages() {
        MessageRepository repository = new MessageRepository(new JsonDataStore(), tempDir.resolve("messages.json"));
        MessageService service = new MessageService(repository);

        MessageRecord first = service.sendMessage("job-01", "apply-01", "ta1", "mo1", "Can I ask about interview timing?");
        MessageRecord reply = service.sendMessage("job-01", "apply-01", "mo1", "ta1", "Tuesday 10:00 works.");

        List<MessageRecord> taMessages = service.getConversationForUser("ta1");
        assertEquals(2, taMessages.size());
        assertEquals(reply.getMessageId(), taMessages.get(0).getMessageId());
        assertEquals("msg-01", first.getMessageId());
        assertFalse(repository.findAll().get(1).isRead());

        service.markAllRead("ta1");

        assertTrue(repository.findAll().stream()
                .filter(message -> "ta1".equals(message.getRecipientUserId()))
                .allMatch(MessageRecord::isRead));
    }

    @Test
    void shouldRejectEmptyMessages() {
        MessageRepository repository = new MessageRepository(new JsonDataStore(), tempDir.resolve("messages.json"));
        MessageService service = new MessageService(repository);

        assertThrows(IllegalArgumentException.class,
                () -> service.sendMessage("job-01", null, "ta1", "mo1", " "));
        assertTrue(repository.findAll().isEmpty());
    }
}
