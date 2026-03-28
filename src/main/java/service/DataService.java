package service;

import model.SystemConfig;
import repository.*;
import util.Constants;
import util.SampleDataLoader;

/**
 * Wires repositories and exposes system-wide operations.
 */
public class DataService {
    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ConfigRepository configRepository;
    private final SampleDataLoader sampleDataLoader;

    public DataService() {
        JsonDataStore dataStore = new JsonDataStore();
        this.userRepository = new UserRepository(dataStore, Constants.USERS_FILE);
        this.profileRepository = new ApplicantProfileRepository(dataStore, Constants.PROFILES_FILE);
        this.jobRepository = new JobRepository(dataStore, Constants.JOBS_FILE);
        this.applicationRepository = new ApplicationRepository(dataStore, Constants.APPLICATIONS_FILE);
        this.configRepository = new ConfigRepository(dataStore, Constants.CONFIG_FILE);
        this.sampleDataLoader = new SampleDataLoader(userRepository, profileRepository, jobRepository, applicationRepository, configRepository);
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public ApplicantProfileRepository getProfileRepository() {
        return profileRepository;
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }

    public ApplicationRepository getApplicationRepository() {
        return applicationRepository;
    }

    public ConfigRepository getConfigRepository() {
        return configRepository;
    }

    public void ensureDataFiles() {
        userRepository.findAll();
        profileRepository.findAll();
        jobRepository.findAll();
        applicationRepository.findAll();
        configRepository.load();
    }

    public void loadSampleData() {
        sampleDataLoader.loadSampleData();
    }

    public void resetData() {
        sampleDataLoader.resetData();
    }

    public SystemConfig getConfig() {
        return configRepository.load();
    }
}
