package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small password hashing helper for the local JSON-backed authentication flow.
 */
public final class PasswordUtil {
    private static final int SHA_256_HEX_LENGTH = 64;

    private PasswordUtil() {
        // Utility class.
    }

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.valueOf(rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 password hashing is not available.", e);
        }
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (isHash(storedPassword)) {
            return hash(rawPassword).equalsIgnoreCase(storedPassword);
        }
        return String.valueOf(rawPassword).equals(storedPassword);
    }

    public static boolean isHash(String storedPassword) {
        return storedPassword != null && storedPassword.matches("(?i)^[0-9a-f]{" + SHA_256_HEX_LENGTH + "}$");
    }
}
