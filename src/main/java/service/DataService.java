package service;

import model.SystemConfig;
import repository.*;
import util.Constants;
import util.SampleDataLoader;

/**
 * The central data service that orchestrates all data operations in the TA Recruitment System.
 * This class acts as a facade and dependency injection container for all repository classes,
 * providing a unified interface for data access throughout the application.
 *
 * Key responsibilities:
 * - Initialize and wire all repository instances with their respective data stores
 * - Provide access to all repositories through getter methods
 * - Ensure data files exist and are properly initialized
 * - Load sample data for testing and demonstration purposes
 * - Reset data to a clean state when needed
 * - Manage system configuration
 *
 * The DataService follows the Repository pattern and Dependency Injection principles,
 * ensuring loose coupling between data access logic and business logic.
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public class DataService {

    /**
     * Repository for managing user accounts and authentication data.
     * Handles CRUD operations for User entities stored in the users JSON file.
     */
    private final UserRepository userRepository;

    /**
     * Repository for managing applicant profiles.
     * Stores detailed information about students applying for TA positions.
     */
    private final ApplicantProfileRepository profileRepository;

    /**
     * Repository for managing job postings.
     * Contains all available TA positions with their requirements and details.
     */
    private final JobRepository jobRepository;

    /**
     * Repository for managing job applications.
     * Tracks which applicants have applied for which positions and their application status.
     */
    private final ApplicationRepository applicationRepository;

    /**
     * Repository for managing system configuration settings.
     * Stores application-wide settings and preferences.
     */
    private final ConfigRepository configRepository;

    /**
     * Utility class for loading sample data into the system.
     * Used for testing, demonstrations, and initial system setup.
     */
    private final SampleDataLoader sampleDataLoader;

    /**
     * Constructs a new DataService instance and initializes all repositories.
     * This constructor sets up the entire data layer by:
     * 1. Creating a shared JsonDataStore instance for all repositories
     * 2. Initializing each repository with its specific data file path
     * 3. Creating a SampleDataLoader with references to all repositories
     *
     * This ensures that all data operations use the same underlying data store
     * and that sample data loading can access all repositories.
     */
    public DataService() {
        // Create a single JsonDataStore instance to be shared across all repositories
        // This ensures consistency and allows for potential future optimizations like caching
        JsonDataStore dataStore = new JsonDataStore();

        // Initialize each repository with the shared data store and their specific file paths
        // File paths are defined as constants to ensure consistency and ease of maintenance
        this.userRepository = new UserRepository(dataStore, Constants.USERS_FILE);
        this.profileRepository = new ApplicantProfileRepository(dataStore, Constants.PROFILES_FILE);
        this.jobRepository = new JobRepository(dataStore, Constants.JOBS_FILE);
        this.applicationRepository = new ApplicationRepository(dataStore, Constants.APPLICATIONS_FILE);
        this.configRepository = new ConfigRepository(dataStore, Constants.CONFIG_FILE);

        // Create the sample data loader with access to all repositories
        // This allows loading predefined test data across all entities
        this.sampleDataLoader = new SampleDataLoader(userRepository, profileRepository, jobRepository, applicationRepository, configRepository);
    }

    /**
     * Gets the user repository for accessing user-related data operations.
     *
     * @return The UserRepository instance for managing users.
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }

    /**
     * Gets the applicant profile repository for accessing profile-related data operations.
     *
     * @return The ApplicantProfileRepository instance for managing applicant profiles.
     */
    public ApplicantProfileRepository getProfileRepository() {
        return profileRepository;
    }

    /**
     * Gets the job repository for accessing job posting data operations.
     *
     * @return The JobRepository instance for managing job postings.
     */
    public JobRepository getJobRepository() {
        return jobRepository;
    }

    /**
     * Gets the application repository for accessing application data operations.
     *
     * @return The ApplicationRepository instance for managing job applications.
     */
    public ApplicationRepository getApplicationRepository() {
        return applicationRepository;
    }

    /**
     * Gets the configuration repository for accessing system settings.
     *
     * @return The ConfigRepository instance for managing system configuration.
     */
    public ConfigRepository getConfigRepository() {
        return configRepository;
    }

    /**
     * Ensures that all required data files exist and are accessible.
     * This method performs lazy initialization by calling findAll() on each repository,
     * which triggers file creation if the files don't exist.
     *
     * This is typically called during application startup to ensure the data layer
     * is ready for use. If any files are missing, they will be created with empty data structures.
     */
    public void ensureDataFiles() {
        // Call findAll() on each repository to trigger file initialization
        // This is a side effect that ensures files exist before they're needed
        userRepository.findAll();
        profileRepository.findAll();
        jobRepository.findAll();
        applicationRepository.findAll();

        // Load configuration, which might have different initialization logic
        configRepository.load();
    }

    /**
     * Loads sample data into the system for testing and demonstration purposes.
     * This method populates all repositories with predefined test data,
     * allowing developers and users to see the system with realistic content.
     *
     * Sample data typically includes:
     * - Test user accounts with different roles
     * - Sample applicant profiles
     * - Example job postings
     * - Mock applications and their statuses
     */
    public void loadSampleData() {
        sampleDataLoader.loadSampleData();
    }

    /**
     * Resets all data in the system to a clean state.
     * This method clears all data files and optionally reloads sample data.
     * Useful for testing scenarios where a known starting state is required.
     *
     * Warning: This operation is destructive and will permanently delete all user data.
     * It should only be used in development or testing environments.
     */
    public void resetData() {
        sampleDataLoader.resetData();
    }

    /**
     * Retrieves the current system configuration.
     * This method loads the latest configuration settings from the data store.
     *
     * @return The SystemConfig object containing current system settings.
     */
    public SystemConfig getConfig() {
        return configRepository.load();
    }
}
