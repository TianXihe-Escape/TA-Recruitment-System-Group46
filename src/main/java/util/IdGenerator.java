package util;

import java.util.UUID;

/**
 * Creates readable prefixed identifiers for persisted records.
 */
public final class IdGenerator {
    private IdGenerator() {
    }

    public static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
