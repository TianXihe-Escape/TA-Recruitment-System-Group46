package service;

import model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.ApplicantProfileRepository;
import repository.JsonDataStore;
import repository.UserRepository;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    @TempDir
    Path tempDir;

    private AuthService authService;
    private UserRepository userRepository;
    private ApplicantProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        JsonDataStore dataStore = new JsonDataStore();
        userRepository = new UserRepository(dataStore, tempDir.resolve("users.json"));
        profileRepository = new ApplicantProfileRepository(dataStore, tempDir.resolve("profiles.json"));
        authService = new AuthService(userRepository, profileRepository, new ValidationService());
    }

    @Test
    void shouldRejectTaRegistrationWithInvalidName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.registerTa("ta@bupt.edu.cn", "1234", "pass123", "pass123"));

        assertEquals(0, userRepository.findAll().size());
        assertEquals(0, profileRepository.findAll().size());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Name can use letters"));
    }

    @Test
    void shouldRejectMoRegistrationWithoutManagedModules() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.registerMo("mo@bupt.edu.cn", "Li Hua", "pass123", "pass123", List.of()));

        assertEquals(0, userRepository.findAll().size());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("At least one managed module code is required"));
    }

    @Test
    void shouldRegisterMoWithNormalizedManagedModules() {
        var user = authService.registerMo(
                "mo@bupt.edu.cn",
                "Li Hua",
                "pass123",
                "pass123",
                List.of(" comp1001 ", "COMP1001", "data2002")
        );

        assertEquals(Role.MO, user.getRole());
        assertEquals(List.of("COMP1001", "DATA2002"), user.getManagedModuleCodes());
    }
}
