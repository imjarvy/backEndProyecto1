package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.FirebaseIdentityDTO;
import com.uc.ms_security.dto.auth.OAuthLoginRequestDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthServiceTests {

    private final FirebaseTokenService firebaseTokenService =
            mock(FirebaseTokenService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final SessionService sessionService = mock(SessionService.class);

    private OAuthService oauthService;

    @BeforeEach
    void setUp() {
        oauthService = new OAuthService(
                firebaseTokenService,
                userRepository,
                jwtService,
                sessionService
        );
    }

    @Test
    void oauthLoginWithKnownUserGeneratesSameInternalToken() {
        OAuthLoginRequestDTO dto = oauthRequest();
        User user = new User();
        user.setId(1L);
        user.setEmail("ana@example.com");
        FirebaseIdentityDTO identity = new FirebaseIdentityDTO(
                "firebase-uid",
                "ana@example.com",
                "Ana",
                "google.com"
        );
        AuthResponseDTO token = new AuthResponseDTO(
                "signed-token",
                "Bearer",
                "token-id",
                Instant.now().plusSeconds(900)
        );

        when(firebaseTokenService.verify(dto.getIdToken()))
                .thenReturn(identity);
        when(userRepository.findByEmail(identity.email()))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(token);

        assertEquals(token, oauthService.login(dto));
        verify(jwtService).generateToken(user);
        verify(sessionService).registerAuthenticationSession(
                user,
                token.tokenId(),
                token.expiresAt()
        );
    }

    @Test
    void oauthLoginWithUnknownUserReturnsGenericUnauthorizedError() {
        OAuthLoginRequestDTO dto = oauthRequest();
        FirebaseIdentityDTO identity = new FirebaseIdentityDTO(
                "firebase-uid",
                "desconocido@example.com",
                "Desconocido",
                "github.com"
        );

        when(firebaseTokenService.verify(dto.getIdToken()))
                .thenReturn(identity);
        when(userRepository.findByEmail(identity.email()))
                .thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> oauthService.login(dto)
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
        assertEquals("Credenciales incorrectas", exception.getMessage());
        verify(jwtService, never()).generateToken(any());
        verify(sessionService, never()).registerAuthenticationSession(
                any(),
                any(),
                any()
        );
    }

    @Test
    void oauthLoginDoesNotGenerateTokenWhenFirebaseValidationFails() {
        OAuthLoginRequestDTO dto = oauthRequest();

        when(firebaseTokenService.verify(dto.getIdToken()))
                .thenThrow(new ApplicationException(
                        ErrorCase.UNAUTHORIZED,
                        "Token de Firebase inválido"
                ));

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> oauthService.login(dto)
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any());
        verify(sessionService, never()).registerAuthenticationSession(
                any(),
                any(),
                any()
        );
    }

    private static OAuthLoginRequestDTO oauthRequest() {
        OAuthLoginRequestDTO dto = new OAuthLoginRequestDTO();
        dto.setIdToken("firebase-id-token");
        return dto;
    }
}
