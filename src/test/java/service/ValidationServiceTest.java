package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationServiceTest {
    private final ValidationService validationService = new ValidationService();

    @Test
    void shouldRejectInvalidEmailRegistration() {
        assertFalse(validationService.validateRegistration("not-an-email", "1234", "1234").isEmpty());
    }

    @Test
    void shouldAcceptValidProfileFields() {
        assertTrue(validationService.validateApplicantProfile("Li Hua", "li@bupt.edu.cn", "13800000000").isEmpty());
    }
}
