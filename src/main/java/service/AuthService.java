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
 * Handles simple authentication and TA registration.
 */
public class AuthService {
    private final UserRepository userRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ValidationService validationService;

    public AuthService(UserRepository userRepository,
                       ApplicantProfileRepository profileRepository,
                       ValidationService validationService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.validationService = validationService;
    }

    public model.User login(String username, String password, Role role) {
        String normalizedUsername = validationService.normalizeEmail(username);
        List<String> errors = validationService.validateLogin(normalizedUsername, password);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        return userRepository.findByUsername(normalizedUsername)
                .filter(user -> user.getPassword().equals(password) && user.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException("Wrong username, password, or role."));
    }

    public model.User registerTa(String username, String name, String password, String confirmPassword) {
        return registerUser(username, name, password, confirmPassword, Role.TA, List.of());
    }

    public model.User registerMo(String username,
                                 String name,
                                 String password,
                                 String confirmPassword,
                                 List<String> managedModuleCodes) {
        return registerUser(username, name, password, confirmPassword, Role.MO, managedModuleCodes);
    }

    private model.User registerUser(String username,
                                    String name,
                                    String password,
                                    String confirmPassword,
                                    Role role,
                                    List<String> managedModuleCodes) {
        String normalizedUsername = validationService.normalizeEmail(username);
        String normalizedName = validationService.normalizePersonName(name);
        List<String> errors = validationService.validateRegistration(normalizedUsername, password, confirmPassword);
        if ((role == Role.MO || role == Role.TA) && normalizedName.isBlank()) {
            errors.add(role == Role.MO ? "MO name is required." : "TA name is required.");
        }
        Optional<model.User> existing = userRepository.findByUsername(normalizedUsername);
        if (existing.isPresent()) {
            errors.add("This username is already registered.");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        List<model.User> users = new ArrayList<>(userRepository.findAll());
        String userId = IdGenerator.nextUserId(role, users);
        model.User user = new model.User(userId, normalizedName, normalizedUsername, password, role, managedModuleCodes);
        users.add(user);
        userRepository.saveAll(users);

        if (role == Role.TA) {
            List<ApplicantProfile> profiles = new ArrayList<>(profileRepository.findAll());
            ApplicantProfile profile = new ApplicantProfile(IdGenerator.nextApplicantId(profiles), user.getUserId());
            profile.setName(normalizedName);
            profile.setEmail(normalizedUsername);
            profiles.add(profile);
            profileRepository.saveAll(profiles);
        }
        return user;
    }
}
