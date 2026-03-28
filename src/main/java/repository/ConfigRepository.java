package repository;

import model.SystemConfig;

import java.nio.file.Path;

/**
 * Repository for simple application configuration.
 */
public class ConfigRepository {
    private final JsonDataStore dataStore;
    private final Path filePath;

    public ConfigRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    public SystemConfig load() {
        return dataStore.readObject(filePath, SystemConfig.class, new SystemConfig());
    }

    public void save(SystemConfig config) {
        dataStore.writeObject(filePath, config);
    }
}
