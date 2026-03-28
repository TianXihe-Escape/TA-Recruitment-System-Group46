package service;

import com.fasterxml.jackson.core.type.TypeReference;
import model.Role;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonDataStore;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonDataStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldWriteAndReadList() {
        JsonDataStore store = new JsonDataStore();
        Path file = tempDir.resolve("users.json");
        List<User> users = List.of(new User("u1", "admin@bupt.edu.cn", "admin123", Role.ADMIN));

        store.writeList(file, users);
        List<User> loaded = store.readList(file, new TypeReference<>() {
        });

        assertEquals(1, loaded.size());
        assertEquals("admin@bupt.edu.cn", loaded.get(0).getUsername());
    }
}
