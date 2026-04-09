package repository;

import model.User;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Repository class responsible for managing User entities in the TA Recruitment System.
 * This class provides data access operations for user accounts, implementing the
 * Repository pattern to abstract data persistence details from business logic.
 *
 * The UserRepository handles all CRUD (Create, Read, Update, Delete) operations
 * related to user management, including:
 * - Retrieving all users
 * - Finding users by username (case-insensitive)
 * - Saving collections of users
 *
 * Key design principles:
 * - Separation of concerns: Data access logic is isolated from business logic
 * - Abstraction: Uses JsonDataStore for actual file I/O operations
 * - Immutability: Methods return defensive copies where appropriate
 * - Type safety: Uses generics and Optional for null-safe operations
 *
 * The repository works with JSON file storage, providing a simple and human-readable
 * persistence mechanism suitable for this application's requirements.
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public class UserRepository {

    /**
     * The data store instance used for JSON file operations.
     * This provides the actual file I/O functionality, allowing the repository
     * to focus on data access logic rather than low-level file operations.
     */
    private final JsonDataStore dataStore;

    /**
     * The file system path to the JSON file containing user data.
     * This path is typically resolved through Constants.USERS_FILE and
     * points to the users.json file in the data directory.
     */
    private final Path filePath;

    /**
     * Constructs a new UserRepository with the specified data store and file path.
     * This constructor initializes the repository with its dependencies,
     * following dependency injection principles for testability and flexibility.
     *
     * @param dataStore The JsonDataStore instance to use for file operations.
     * @param filePath  The Path to the JSON file containing user data.
     */
    public UserRepository(JsonDataStore dataStore, Path filePath) {
        this.dataStore = dataStore;
        this.filePath = filePath;
    }

    /**
     * Retrieves all users from the data store.
     * This method reads the entire users.json file and deserializes it into a list of User objects.
     * If the file doesn't exist, it will be created with an empty list.
     *
     * Note: This method returns a direct reference to the deserialized list.
     * Clients should treat this as read-only to avoid unintended side effects.
     *
     * @return A List containing all User objects stored in the system.
     *         Returns an empty list if no users exist or the file cannot be read.
     */
    public List<User> findAll() {
        return dataStore.readList(filePath, User.class);
    }

    /**
     * Finds a user by their username using case-insensitive matching.
     * This method searches through all users to find one with a matching username,
     * ignoring case differences to provide user-friendly authentication.
     *
     * The search is performed using Java 8 streams for functional programming style
     * and efficient filtering. The method returns an Optional to handle cases
     * where no user is found with the specified username.
     *
     * @param username The username to search for (case-insensitive).
     * @return An Optional containing the User if found, or empty if no matching user exists.
     */
    public Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))  // Case-insensitive comparison
                .findFirst();  // Return the first match (usernames should be unique)
    }

    /**
     * Saves all users to the data store.
     * This method serializes the provided list of users to JSON format
     * and writes it to the users.json file, replacing any existing content.
     *
     * This operation is atomic - either all users are saved successfully,
     * or the operation fails and the file remains unchanged.
     *
     * @param users The List of User objects to save. Must not be null.
     * @throws RuntimeException if the save operation fails due to I/O errors
     *                          or JSON serialization issues.
     */
    public void saveAll(List<User> users) {
        dataStore.writeList(filePath, users);
    }
}
