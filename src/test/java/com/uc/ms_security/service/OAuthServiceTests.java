package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.FirebaseIdentityDTO;
import com.uc.ms_security.dto.auth.OAuthLoginRequestDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthServiceTests {

    private final TokenVerifier tokenVerifier = mock(TokenVerifier.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserRoleRepository userRoleRepository =
            mock(UserRoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);

    private OAuthService oauthService;

    @BeforeEach
    void setUp() {
        oauthService = new OAuthService(
                tokenVerifier,
                userRepository,
                roleRepository,
                userRoleRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void loginWithVerifiedIdentityGeneratesTokenForMatchingUser() {
        OAuthLoginRequestDTO request = loginRequest();
        User user = new User();
        AuthResponseDTO response = new AuthResponseDTO(
                "signed-token",
                "Bearer",
                Instant.now().plusSeconds(900)
        );

        when(tokenVerifier.verify("id-token"))
                .thenReturn(identity());
        when(userRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(response);

        assertEquals(response, oauthService.login(request));
        verify(tokenVerifier).verify("id-token");
        verify(jwtService).generateToken(user);
    }

    @Test
    void firstLoginCreatesUserAndAssignsConsultantRole() {
        OAuthLoginRequestDTO request = loginRequest();
        Role role = new Role();
        User user = new User();
        AuthResponseDTO response = new AuthResponseDTO(
                "signed-token",
                "Bearer",
                Instant.now().plusSeconds(900)
        );

        when(tokenVerifier.verify("id-token"))
                .thenReturn(identity());
        when(userRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByNameIgnoreCase("CONSULTOR"))
                .thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            user.setName(savedUser.getName());
            user.setEmail(savedUser.getEmail());
            user.setPassword(savedUser.getPassword());
            return user;
        });
        when(jwtService.generateToken(user)).thenReturn(response);

        assertEquals(response, oauthService.login(request));

        assertEquals("Ana Pérez", user.getName());
        assertEquals("ana@example.com", user.getEmail());
        assertEquals("encoded-random-password", user.getPassword());
        verify(passwordEncoder).encode(any(String.class));
        verify(userRoleRepository).save(any(UserRole.class));
        verify(jwtService).generateToken(user);
    }

    @Test
    void loginWithIdentityWithoutNameUsesEmailPrefix() {
        FirebaseIdentityDTO identity =
                new FirebaseIdentityDTO(
                        "firebase-uid",
                        "ana@example.com",
                        null,
                        "google.com"
                );
        when(tokenVerifier.verify("id-token")).thenReturn(identity);
        when(userRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.empty());
        when(roleRepository.findByNameIgnoreCase("CONSULTOR"))
                .thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class))).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        oauthService.login(loginRequest());

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                user -> "ana".equals(user.getName())
        ));
    }

    @Test
    void existingUserDoesNotReceiveAnotherRoleOrPasswordChange() {
        User user = new User();
        user.setPassword("existing-password-hash");
        when(tokenVerifier.verify("id-token")).thenReturn(identity());
        when(userRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(user));

        oauthService.login(loginRequest());

        assertEquals("existing-password-hash", user.getPassword());
        verify(roleRepository, org.mockito.Mockito.never())
                .findByNameIgnoreCase(any(String.class));
        verify(userRoleRepository, org.mockito.Mockito.never())
                .save(any(UserRole.class));
    }

    private static FirebaseIdentityDTO identity() {
        return new FirebaseIdentityDTO(
                "firebase-uid",
                "ana@example.com",
                "Ana Pérez",
                "google.com"
        );
    }

    private static OAuthLoginRequestDTO loginRequest() {
        OAuthLoginRequestDTO request = new OAuthLoginRequestDTO();
        request.setIdToken("id-token");
        return request;
    }
}
