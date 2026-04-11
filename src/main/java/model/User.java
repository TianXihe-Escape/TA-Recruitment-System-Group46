package model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Represents a user account in the TA Recruitment System.
 * This class encapsulates all the information related to a user, including authentication details,
 * personal information, role-based permissions, and module management responsibilities.
 * Users can be students, faculty members, or administrators, each with different access levels.
 *
 * The User class is persisted in the file store and is used throughout the application
 * for authentication, authorization, and personalization of the user interface.
 *
 * Key features:
 * - Unique user identification
 * - Authentication credentials (username and password)
 * - Role-based access control
 * - Management of specific course modules (for faculty)
 * - Data validation and normalization
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public class User {

    /**
     * Unique identifier for the user.
     * This is typically a system-generated ID that remains constant throughout the user's lifecycle.
     * Used for database operations and as a primary key in data storage.
     */
    private String userId;

    /**
     * The full name of the user.
     * This field stores the display name of the user, which may include first and last names.
     * It is used for personalization in the UI and reports.
     */
    private String name;

    /**
     * The username used for authentication.
     * This must be unique across all users in the system and is used during login.
     * Typically, this could be an email address or a custom username.
     */
    private String username;

    /**
     * The password for user authentication.
     * This field stores the hashed password (in a real system, it should be properly encrypted).
     * For security reasons, plain text passwords should never be stored.
     */
    private String password;

    /**
     * The role of the user in the system.
     * Determines the permissions and access levels. Possible roles include STUDENT, FACULTY, ADMIN, etc.
     * This field is crucial for implementing role-based access control (RBAC).
     */
    private Role role;

    /**
     * List of module codes that this user (typically a faculty member) manages.
     * Each module code represents a course or subject that the user has administrative control over.
     * This is used to filter job postings, applications, and workload data relevant to the user.
     * The list is maintained in a normalized, uppercase format to ensure consistency.
     */
    private List<String> managedModuleCodes = new ArrayList<>();

    /**
     * Default constructor for the User class.
     * Creates an empty User object that can be populated using setter methods.
     * This is useful for deserialization from JSON or creating users programmatically.
     */
    public User() {
    }

    /**
     * Constructs a User with basic information including user ID, name, username, password, and role.
     * This constructor is used when creating a new user with all essential details.
     *
     * @param userId   The unique identifier for the user.
     * @param name     The full name of the user.
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @param role     The role of the user in the system.
     */
    public User(String userId, String name, String username, String password, Role role) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Constructs a User with user ID, username, password, and role, but without a name.
     * This constructor calls the main constructor with null for the name parameter.
     * Useful when the name is not immediately available or optional.
     *
     * @param userId   The unique identifier for the user.
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @param role     The role of the user in the system.
     */
    public User(String userId, String username, String password, Role role) {
        this(userId, null, username, password, role);
    }

    /**
     * Constructs a User with user ID, username, password, role, and managed module codes.
     * This constructor is suitable for faculty users who manage specific modules.
     * It calls the previous constructor and then sets the managed module codes.
     *
     * @param userId              The unique identifier for the user.
     * @param username            The username for authentication.
     * @param password            The password for authentication.
     * @param role                The role of the user in the system.
     * @param managedModuleCodes  List of module codes this user manages.
     */
    public User(String userId, String username, String password, Role role, List<String> managedModuleCodes) {
        this(userId, username, password, role);
        setManagedModuleCodes(managedModuleCodes);
    }

    /**
     * Constructs a User with all fields including name and managed module codes.
     * This is the most complete constructor, providing all user information at creation time.
     *
     * @param userId              The unique identifier for the user.
     * @param name                The full name of the user.
     * @param username            The username for authentication.
     * @param password            The password for authentication.
     * @param role                The role of the user in the system.
     * @param managedModuleCodes  List of module codes this user manages.
     */
    public User(String userId, String name, String username, String password, Role role, List<String> managedModuleCodes) {
        this(userId, name, username, password, role);
        setManagedModuleCodes(managedModuleCodes);
    }

    /**
     * Gets the unique user identifier.
     *
     * @return The user ID as a String.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique user identifier.
     *
     * @param userId The new user ID to set.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the full name of the user.
     *
     * @return The user's name as a String, or null if not set.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the full name of the user.
     *
     * @param name The new name to set for the user.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the username used for authentication.
     *
     * @return The username as a String.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username for authentication.
     *
     * @param username The new username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password for authentication.
     * Note: In a production system, passwords should be hashed and not retrievable in plain text.
     *
     * @return The password as a String.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for authentication.
     *
     * @param password The new password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the role of the user in the system.
     *
     * @return The user's role as a Role enum value.
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets the role of the user in the system.
     *
     * @param role The new role to assign to the user.
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Gets a copy of the list of managed module codes.
     * Returns a defensive copy to prevent external modification of the internal list.
     *
     * @return A new ArrayList containing the managed module codes.
     */
    public List<String> getManagedModuleCodes() {
        return new ArrayList<>(managedModuleCodes);
    }

    /**
     * Sets the list of managed module codes for this user.
     * This method performs normalization on the input list:
     * - Removes null values
     * - Trims whitespace
     * - Converts to uppercase
     * - Removes duplicates while preserving order
     * - Filters out blank strings
     *
     * @param managedModuleCodes The list of module codes to set. Can be null.
     */
    public void setManagedModuleCodes(List<String> managedModuleCodes) {
        // Use LinkedHashSet to maintain insertion order and remove duplicates
        LinkedHashSet<String> normalizedCodes = new LinkedHashSet<>();
        if (managedModuleCodes != null) {
            for (String code : managedModuleCodes) {
                if (code != null) {
                    // Normalize the code: trim whitespace and convert to uppercase
                    String normalized = code.trim().toUpperCase(Locale.ROOT);
                    // Only add non-blank codes
                    if (!normalized.isBlank()) {
                        normalizedCodes.add(normalized);
                    }
                }
            }
        }
        // Store the normalized codes in a new ArrayList
        this.managedModuleCodes = new ArrayList<>(normalizedCodes);
    }

    /**
     * Checks if this user manages a specific module.
     * Performs case-insensitive comparison after normalization.
     *
     * @param moduleCode The module code to check. Can be null.
     * @return true if the user manages the specified module, false otherwise.
     */
    public boolean managesModule(String moduleCode) {
        if (moduleCode == null) {
            return false;
        }
        // Normalize the input module code for comparison
        String normalized = moduleCode.trim().toUpperCase(Locale.ROOT);
        // Check if the normalized code is in the managed modules list
        return managedModuleCodes.contains(normalized);
    }
}
