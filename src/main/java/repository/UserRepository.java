package repository;

import com.fasterxml.jackson.core.type.TypeReference;
import model.User;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Repository for login accounts.
 */
public class UserRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public UserRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public List<User> findAll() {
        return dataStore.readList(filePath, new TypeReference<>() {
        });
    }

    public Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public void saveAll(List<User> users) {
        dataStore.writeList(filePath, users);
    }
}
