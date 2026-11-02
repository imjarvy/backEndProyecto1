package com.uc.ms_security.dto.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetRequestValidationTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsPasswordMatchingExistingRules() {
        assertTrue(validator.validate(
                new PasswordResetRequestDTO("token", "ClaveSegura123!")
        ).isEmpty());
    }

    @Test
    void rejectsPasswordMissingRequiredCharacterTypes() {
        assertFalse(validator.validate(
                new PasswordResetRequestDTO("token", "clavesegura123!")
        ).isEmpty());
    }

    @Test
    void rejectsPasswordContainingSpaces() {
        assertFalse(validator.validate(
                new PasswordResetRequestDTO("token", "Clave Segura123!")
        ).isEmpty());
    }
}
