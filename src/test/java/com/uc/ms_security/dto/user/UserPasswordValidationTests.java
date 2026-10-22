package com.uc.ms_security.dto.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPasswordValidationTests {

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
    void createAcceptsStrongPassword() {
        CreateUserDTO dto = createUser("ClaveSegura123!");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void createRejectsPasswordWithoutRequiredCharacters() {
        CreateUserDTO dto = createUser("clavesegura123!");

        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void updateAllowsOmittingPassword() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("Ana");
        dto.setEmail("ana@example.com");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void updateRejectsPasswordContainingSpaces() {
        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("Ana");
        dto.setEmail("ana@example.com");
        dto.setPassword("Clave Segura123!");

        assertFalse(validator.validate(dto).isEmpty());
    }

    private static CreateUserDTO createUser(String password) {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setName("Ana");
        dto.setEmail("ana@example.com");
        dto.setPassword(password);
        return dto;
    }
}
