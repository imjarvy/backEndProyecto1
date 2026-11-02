package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.PasswordRecoveryRequestDTO;
import com.uc.ms_security.dto.auth.PasswordResetRequestDTO;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.PasswordRecoveryToken;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.repository.PasswordRecoveryTokenRepository;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import org.mockito.ArgumentCaptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordRecoveryServiceTests {

    private static final String RAW_TOKEN = "test-recovery-token";
    private static final String NEW_PASSWORD = "NuevaClave123!";

    private final PasswordRecoveryTokenService tokenService =
            mock(PasswordRecoveryTokenService.class);
    private final PasswordRecoveryTokenRepository tokenRepository =
            mock(PasswordRecoveryTokenRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(4);
    private final SessionService sessionService = mock(SessionService.class);
    private final NotificationService notificationService =
            mock(NotificationService.class);

    private PasswordRecoveryService passwordRecoveryService;

    @BeforeEach
    void setUp() {
        passwordRecoveryService = new PasswordRecoveryService(
                tokenService,
                tokenRepository,
                userRepository,
                passwordEncoder,
                sessionService,
                notificationService
        );
        ReflectionTestUtils.setField(
                passwordRecoveryService,
                "resetUrl",
                "http://localhost:4200/authentication/reset-password"
        );
    }

    @Test
    void unknownEmailReceivesTheNeutralResponseWithoutSendingEmail() {
        when(tokenService.issueForLocalUser(
                eq("unknown@example.com"),
                anyString()
        )).thenReturn(Optional.empty());

        String response = passwordRecoveryService.requestRecovery(
                new PasswordRecoveryRequestDTO("unknown@example.com")
        );

        assertEquals(
                "Solicitud recibida. Si la cuenta es compatible, el sistema "
                        + "intentará enviar instrucciones de recuperación.",
                response
        );
        verifyNoInteractions(notificationService);
    }

    @Test
    void expiredTokenIsRejectedWithoutChangingPasswordOrSessions() {
        User user = localUser(17L);
        PasswordRecoveryToken token = token(user);
        token.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(tokenRepository.findByTokenHashForUpdate(hash(RAW_TOKEN)))
                .thenReturn(Optional.of(token));

        assertThrows(
                ResponseStatusException.class,
                () -> passwordRecoveryService.resetPassword(resetRequest())
        );

        verify(userRepository, never()).save(user);
        verifyNoInteractions(sessionService);
    }

    @Test
    void usedTokenIsRejectedWithoutChangingPasswordOrSessions() {
        User user = localUser(17L);
        PasswordRecoveryToken token = token(user);
        token.setUsedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(tokenRepository.findByTokenHashForUpdate(hash(RAW_TOKEN)))
                .thenReturn(Optional.of(token));

        assertThrows(
                ResponseStatusException.class,
                () -> passwordRecoveryService.resetPassword(resetRequest())
        );

        verify(userRepository, never()).save(user);
        verifyNoInteractions(sessionService);
    }

    @Test
    void successfulResetStoresBCryptPasswordConsumesTokenAndRevokesSessions() {
        User user = localUser(17L);
        String existingHash = passwordEncoder.encode("Anterior123!");
        user.setPassword(existingHash);
        PasswordRecoveryToken token = token(user);
        token.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        when(tokenRepository.findByTokenHashForUpdate(hash(RAW_TOKEN)))
                .thenReturn(Optional.of(token));

        passwordRecoveryService.resetPassword(resetRequest());

        assertNotEquals(existingHash, user.getPassword());
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, user.getPassword()));
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
        verify(sessionService).revokeActiveSessionsByUserId(17L);
    }

    @Test
    void emailDeliveryFailureInvalidatesTheIssuedToken() {
        PasswordRecoveryToken token = token(localUser(17L));
        when(tokenService.issueForLocalUser(
                org.mockito.ArgumentMatchers.eq("ana@example.com"),
                anyString()
        )).thenReturn(Optional.of(token));
        org.mockito.Mockito.doThrow(new ApplicationException(
                com.uc.ms_security.exception.ErrorCase.INVALID_OPERATION,
                "Servicio de notificaciones no disponible"
        )).when(notificationService).requestPasswordRecoveryEmail(
                org.mockito.ArgumentMatchers.eq("ana@example.com"),
                anyString()
        );

        String response = passwordRecoveryService.requestRecovery(
                new PasswordRecoveryRequestDTO("ana@example.com")
        );

        assertEquals(
                "Solicitud recibida. Si la cuenta es compatible, el sistema "
                        + "intentará enviar instrucciones de recuperación.",
                response
        );
        ArgumentCaptor<String> tokenHash = ArgumentCaptor.forClass(
                String.class
        );
        verify(tokenService).issueForLocalUser(
                eq("ana@example.com"),
                tokenHash.capture()
        );
        verify(tokenService).invalidateByHash(tokenHash.getValue());
        assertNotEquals(RAW_TOKEN, tokenHash.getValue());
    }

    private PasswordRecoveryToken token(User user) {
        PasswordRecoveryToken token = new PasswordRecoveryToken();
        token.setUser(user);
        token.setTokenHash(hash(RAW_TOKEN));
        token.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        token.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        return token;
    }

    private User localUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("ana@example.com");
        user.setAuthProvider(AuthProvider.LOCAL_PASSWORD);
        return user;
    }

    private PasswordResetRequestDTO resetRequest() {
        return new PasswordResetRequestDTO(RAW_TOKEN, NEW_PASSWORD);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
