package repository;

import com.fasterxml.jackson.core.type.TypeReference;
import util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared JSON persistence helper for collections and config objects.
 */
public class JsonDataStore {
    public <T> List<T> readList(Path path, TypeReference<List<T>> typeReference) {
        initializeListFile(path);
        try {
            return JsonUtil.getMapper().readValue(path.toFile(), typeReference);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read data from " + path, e);
        }
    }

    public <T> void writeList(Path path, List<T> values) {
        ensureParent(path);
        try {
            JsonUtil.getMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), values);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write data to " + path, e);
        }
    }

    public <T> T readObject(Path path, Class<T> clazz, T defaultValue) {
        initializeObjectFile(path, defaultValue);
        try {
            return JsonUtil.getMapper().readValue(path.toFile(), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config from " + path, e);
        }
    }

    public <T> void writeObject(Path path, T value) {
        ensureParent(path);
        try {
            JsonUtil.getMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write config to " + path, e);
        }
    }

    private void initializeListFile(Path path) {
        ensureParent(path);
        if (!Files.exists(path)) {
            writeList(path, new ArrayList<>());
        }
    }

    private <T> void initializeObjectFile(Path path, T defaultValue) {
        ensureParent(path);
        if (!Files.exists(path)) {
            writeObject(path, defaultValue);
        }
    }

    private void ensureParent(Path path) {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize " + path, e);
        }
    }
}
