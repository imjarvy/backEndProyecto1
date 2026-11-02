package com.uc.ms_security.service;

import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.PasswordRecoveryToken;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.repository.PasswordRecoveryTokenRepository;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordRecoveryTokenServiceTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordRecoveryTokenRepository tokenRepository =
            mock(PasswordRecoveryTokenRepository.class);
    private final PasswordRecoveryTokenService tokenService =
            new PasswordRecoveryTokenService(userRepository, tokenRepository);

    @Test
    void unknownEmailDoesNotCreateAToken() {
        when(userRepository.findByEmailForUpdate("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertTrue(tokenService.issueForLocalUser(
                "unknown@example.com",
                "token-hash"
        ).isEmpty());

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void oauthUserDoesNotReceiveLocalRecoveryToken() {
        User user = new User();
        user.setId(18L);
        user.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmailForUpdate("oauth@example.com"))
                .thenReturn(Optional.of(user));

        assertTrue(tokenService.issueForLocalUser(
                "oauth@example.com",
                "token-hash"
        ).isEmpty());

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void issuingTokenInvalidatesPreviousTokensAndExpiresInThirtyMinutes() {
        User user = new User();
        user.setId(19L);
        user.setAuthProvider(AuthProvider.LOCAL_PASSWORD);
        when(userRepository.findByEmailForUpdate("local@example.com"))
                .thenReturn(Optional.of(user));
        when(tokenRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PasswordRecoveryToken token = tokenService.issueForLocalUser(
                "local@example.com",
                "token-hash"
        ).orElseThrow();

        verify(tokenRepository).deleteAllByUserId(19L);
        assertEquals("token-hash", token.getTokenHash());
        assertEquals(
                Duration.ofMinutes(30),
                Duration.between(token.getCreatedAt(), token.getExpiresAt())
        );
        assertTrue(token.getCreatedAt().isBefore(
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1)
        ));
    }
}
