package repository;

import model.MessageRecord;

import java.nio.file.Path;
import java.util.List;

/**
 * Repository for TA/MO message records.
 */
public class MessageRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public MessageRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public List<MessageRecord> findAll() {
        return dataStore.readList(filePath, MessageRecord.class);
    }

    public void saveAll(List<MessageRecord> messages) {
        dataStore.writeList(filePath, messages);
    }
}
