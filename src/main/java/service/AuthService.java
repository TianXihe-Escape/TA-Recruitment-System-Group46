package service;

import model.ApplicantProfile;
import model.Role;
import model.User;
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
        List<String> errors = validationService.validateLogin(username, password);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        return userRepository.findByUsername(username.trim())
                .filter(user -> user.getPassword().equals(password) && user.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException("Wrong username, password, or role."));
    }

    public model.User registerTa(String username, String password, String confirmPassword) {
        List<String> errors = validationService.validateRegistration(username, password, confirmPassword);
        Optional<model.User> existing = userRepository.findByUsername(username.trim());
        if (existing.isPresent()) {
            errors.add("This username is already registered.");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        List<model.User> users = new ArrayList<>(userRepository.findAll());
        model.User user = new model.User(IdGenerator.newId("user"), username.trim(), password, Role.TA);
        users.add(user);
        userRepository.saveAll(users);

        List<ApplicantProfile> profiles = new ArrayList<>(profileRepository.findAll());
        ApplicantProfile profile = new ApplicantProfile(IdGenerator.newId("applicant"), user.getUserId());
        profile.setEmail(username.trim());
        profiles.add(profile);
        profileRepository.saveAll(profiles);
        return user;
    }
}
