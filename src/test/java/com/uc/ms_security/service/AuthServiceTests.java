package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.LoginRequestDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void loginWithValidCredentialsGeneratesToken() {
        LoginRequestDTO dto = loginRequest();
        User user = new User();
        user.setPassword("$2a$12$encoded");
        AuthResponseDTO token = new AuthResponseDTO(
                "signed-token",
                "Bearer",
                Instant.now().plusSeconds(900)
        );

        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(token);

        assertEquals(token, authService.login(dto));
        verify(jwtService).generateToken(user);
    }

    @Test
    void loginWithUnknownEmailReturnsGenericUnauthorizedError() {
        LoginRequestDTO dto = loginRequest();
        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> authService.login(dto)
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
        assertEquals("Credenciales incorrectas", exception.getMessage());
        verify(passwordEncoder, never()).matches(
                dto.getPassword(),
                null
        );
        verify(jwtService, never()).generateToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginWithIncorrectPasswordReturnsSameUnauthorizedError() {
        LoginRequestDTO dto = loginRequest();
        User user = new User();
        user.setPassword("$2a$12$encoded");

        when(userRepository.findByEmail(dto.getEmail()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.getPassword(), user.getPassword()))
                .thenReturn(false);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> authService.login(dto)
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
        assertEquals("Credenciales incorrectas", exception.getMessage());
        verify(jwtService, never()).generateToken(org.mockito.ArgumentMatchers.any());
    }

    private static LoginRequestDTO loginRequest() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("ana@example.com");
        dto.setPassword("ClaveSegura123!");
        return dto;
    }
}
