package service;

import model.ApplicantProfile;
import model.Role;
import repository.ApplicantProfileRepository;
import repository.UserRepository;
import util.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class responsible for handling user authentication and registration in the TA Recruitment System.
 * This class provides a centralized interface for all authentication-related operations,
 * including user login, TA registration, and MO (Module Organizer) registration.
 *
 * Key responsibilities:
 * - User authentication with username, password, and role validation
 * - Registration of new Teaching Assistant accounts
 * - Registration of new Module Organizer accounts with module management permissions
 * - Input validation and normalization
 * - Automatic creation of applicant profiles for TA users
 * - Duplicate username prevention
 * - Secure password handling (basic implementation)
 *
 * The service follows security best practices by:
 * - Normalizing and validating all user inputs
 * - Checking for existing users before registration
 * - Using role-based authentication
 * - Providing clear error messages for failed operations
 *
 * Note: This implementation uses simple password comparison. In a production system,
 * passwords should be hashed using a secure algorithm like bcrypt or Argon2.
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public class AuthService {

    /**
     * Repository for accessing and managing user account data.
     * Used for finding existing users and saving new registrations.
     */
    private final UserRepository userRepository;

    /**
     * Repository for accessing and managing applicant profile data.
     * Used for creating profiles automatically when TA users register.
     */
    private final ApplicantProfileRepository profileRepository;

    /**
     * Service for validating and normalizing user input data.
     * Handles email normalization, password validation, and other input checks.
     */
    private final ValidationService validationService;

    /**
     * Constructs an AuthService with the required dependencies.
     * This constructor follows dependency injection principles, allowing for
     * easier testing and flexibility in component composition.
     *
     * @param userRepository      The repository for user data access operations.
     * @param profileRepository   The repository for applicant profile operations.
     * @param validationService   The service for input validation and normalization.
     */
    public AuthService(UserRepository userRepository,
                       ApplicantProfileRepository profileRepository,
                       ValidationService validationService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.validationService = validationService;
    }

    /**
     * Authenticates a user with the provided credentials and role.
     * This method performs comprehensive validation and authentication checks:
     * 1. Normalizes the username (email) format
     * 2. Validates login credentials using the validation service
     * 3. Searches for the user by normalized username
     * 4. Verifies password and role match
     *
     * If authentication fails at any step, an IllegalArgumentException is thrown
     * with a descriptive error message.
     *
     * @param username The user's username (typically an email address).
     * @param password The user's password in plain text.
     * @param role     The expected role of the user (TA, MO, or ADMIN).
     * @return The authenticated User object if login is successful.
     * @throws IllegalArgumentException if validation fails or credentials are incorrect.
     */
    public model.User login(String username, String password, Role role) {
        // Normalize the username to ensure consistent email formatting
        String normalizedUsername = validationService.normalizeEmail(username);

        // Validate the login credentials and collect any errors
        List<String> errors = validationService.validateLogin(normalizedUsername, password);

        // If validation errors exist, throw an exception with all error messages
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        // Attempt to find the user and verify credentials
        return userRepository.findByUsername(normalizedUsername)
                .filter(user -> user.getPassword().equals(password) && user.getRole() == role)  // Verify password and role
                .orElseThrow(() -> new IllegalArgumentException("Wrong username, password, or role."));  // Authentication failed
    }

    /**
     * Registers a new Teaching Assistant account.
     * This is a convenience method that calls the general registration method
     * with TA-specific parameters (no managed modules).
     *
     * @param username        The desired username (email address) for the new TA.
     * @param name            The full name of the TA.
     * @param password        The password for the account.
     * @param confirmPassword The password confirmation (must match password).
     * @return The newly created User object representing the registered TA.
     * @throws IllegalArgumentException if registration validation fails.
     */
    public model.User registerTa(String username, String name, String password, String confirmPassword) {
        return registerUser(username, name, password, confirmPassword, Role.TA, List.of());
    }

    /**
     * Registers a new Module Organizer account with specified managed modules.
     * This method allows faculty members to register as MOs with the ability
     * to manage specific course modules.
     *
     * @param username            The desired username (email address) for the new MO.
     * @param name                The full name of the MO.
     * @param password            The password for the account.
     * @param confirmPassword     The password confirmation (must match password).
     * @param managedModuleCodes  List of module codes this MO will manage.
     * @return The newly created User object representing the registered MO.
     * @throws IllegalArgumentException if registration validation fails.
     */
    public model.User registerMo(String username,
                                 String name,
                                 String password,
                                 String confirmPassword,
                                 List<String> managedModuleCodes) {
        return registerUser(username, name, password, confirmPassword, Role.MO, managedModuleCodes);
    }

    /**
     * Private helper method that performs the actual user registration logic.
     * This method handles the common registration process for both TA and MO users,
     * including validation, duplicate checking, ID generation, and data persistence.
     *
     * The registration process includes:
     * 1. Input normalization and validation
     * 2. Duplicate username checking
     * 3. User ID generation
     * 4. User creation and persistence
     * 5. Automatic applicant profile creation for TA users
     *
     * @param username            The normalized username for the new user.
     * @param name                The normalized name of the user.
     * @param password            The password for the account.
     * @param confirmPassword     The password confirmation.
     * @param role                The role of the new user (TA or MO).
     * @param managedModuleCodes  List of module codes for MO users (empty for TA).
     * @return The newly created and persisted User object.
     * @throws IllegalArgumentException if any validation fails.
     */
    private model.User registerUser(String username,
                                    String name,
                                    String password,
                                    String confirmPassword,
                                    Role role,
                                    List<String> managedModuleCodes) {
        // Normalize input data for consistency
        String normalizedUsername = validationService.normalizeEmail(username);
        String normalizedName = validationService.normalizePersonName(name);

        // Validate registration data
        List<String> errors = validationService.validateRegistration(normalizedUsername, password, confirmPassword);

        // Validate the display name early so newly created accounts don't start in a
        // state that later profile-editing rules would reject.
        if (role == Role.MO || role == Role.TA) {
            errors.addAll(validationService.validatePersonName(normalizedName));
        }

        // MO accounts must be provisioned with at least one manageable module;
        // otherwise the account can log in but cannot actually manage hiring.
        if (role == Role.MO) {
            errors.addAll(validationService.validateManagedModuleCodes(managedModuleCodes));
        }

        // Check for existing user with the same username
        Optional<model.User> existing = userRepository.findByUsername(normalizedUsername);
        if (existing.isPresent()) {
            errors.add("This username is already registered.");
        }

        // If any errors were found, throw exception
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        // Get all existing users to generate a unique ID
        List<model.User> users = new ArrayList<>(userRepository.findAll());

        // Generate a unique user ID based on role and existing users
        String userId = IdGenerator.nextUserId(role, users);

        // Create the new user object
        model.User user = new model.User(userId, normalizedName, normalizedUsername, password, role, managedModuleCodes);

        // Add the new user to the list and save
        users.add(user);
        userRepository.saveAll(users);

        // For TA users, automatically create an applicant profile
        if (role == Role.TA) {
            List<ApplicantProfile> profiles = new ArrayList<>(profileRepository.findAll());
            // Generate unique applicant ID
            ApplicantProfile profile = new ApplicantProfile(IdGenerator.nextApplicantId(profiles), user.getUserId());
            profile.setName(normalizedName);
            profile.setEmail(normalizedUsername);
            // Add and save the profile
            profiles.add(profile);
            profileRepository.saveAll(profiles);
        }

        // Return the newly created user
        return user;
    }
}
