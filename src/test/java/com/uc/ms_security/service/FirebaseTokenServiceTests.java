package com.uc.ms_security.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.uc.ms_security.dto.auth.FirebaseIdentityDTO;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.FirebaseIdentityMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FirebaseTokenServiceTests {

    private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    private final FirebaseToken firebaseToken = mock(FirebaseToken.class);

    private final FirebaseTokenService firebaseTokenService =
            new FirebaseTokenService(
                    Optional.of(firebaseAuth),
                    new FirebaseIdentityMapper(),
                    true
            );

    @Test
    void validTokenFromAllowedProviderReturnsIdentity() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token", true))
                .thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn("firebase-uid");
        when(firebaseToken.getEmail()).thenReturn("ana@example.com");
        when(firebaseToken.getName()).thenReturn("Ana");
        when(firebaseToken.getClaims()).thenReturn(Map.of(
                "firebase",
                Map.of("sign_in_provider", "google.com")
        ));

        FirebaseIdentityDTO identity =
                firebaseTokenService.verify("id-token");

        assertEquals("firebase-uid", identity.uid());
        assertEquals("ana@example.com", identity.email());
        assertEquals("Ana", identity.name());
        assertEquals("google.com", identity.provider());
    }

    @Test
    void tokenWithoutEmailIsRejected() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token", true))
                .thenReturn(firebaseToken);
        when(firebaseToken.getEmail()).thenReturn(null);
        when(firebaseToken.getClaims()).thenReturn(Map.of(
                "firebase",
                Map.of("sign_in_provider", "microsoft.com")
        ));

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> firebaseTokenService.verify("id-token")
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
    }

    @Test
    void tokenFromDisallowedProviderIsRejected() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token", true))
                .thenReturn(firebaseToken);
        when(firebaseToken.getEmail()).thenReturn("ana@example.com");
        when(firebaseToken.getClaims()).thenReturn(Map.of(
                "firebase",
                Map.of("sign_in_provider", "password")
        ));

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> firebaseTokenService.verify("id-token")
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
        assertEquals(
                "Proveedor de autenticación no permitido",
                exception.getMessage()
        );
    }

    @Test
    void sdkFailureIsReportedAsGenericUnauthorized() throws Exception {
        when(firebaseAuth.verifyIdToken("id-token", true))
                .thenThrow(new FirebaseAuthException(
                        ErrorCode.UNAUTHENTICATED,
                        "internal firebase detail",
                        null,
                        null,
                        AuthErrorCode.REVOKED_ID_TOKEN
                ));

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> firebaseTokenService.verify("id-token")
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
        assertEquals("Token de Firebase inválido", exception.getMessage());
    }

    @Test
    void blankTokenIsRejectedWithoutCallingFirebase() {
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> firebaseTokenService.verify(" ")
        );

        assertEquals(ErrorCase.UNAUTHORIZED, exception.getErrorCase());
        verifyNoInteractions(firebaseAuth);
    }

    @Test
    void unconfiguredFirebaseIsReportedAsInvalidOperation() {
        FirebaseTokenService service = new FirebaseTokenService(
                Optional.empty(),
                new FirebaseIdentityMapper(),
                true
        );

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> service.verify("id-token")
        );

        assertEquals(ErrorCase.INVALID_OPERATION, exception.getErrorCase());
    }
}
