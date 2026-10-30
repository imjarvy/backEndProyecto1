package com.uc.ms_security.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.uc.ms_security.dto.auth.FirebaseIdentityDTO;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.FirebaseIdentityMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;

@Service
public class FirebaseTokenService implements TokenVerifier {

    private static final Set<String> ALLOWED_PROVIDERS = Set.of(
            "google.com",
            "microsoft.com",
            "github.com"
    );

    private final Optional<FirebaseAuth> firebaseAuth;
    private final FirebaseIdentityMapper firebaseIdentityMapper;
    private final boolean checkRevoked;

    public FirebaseTokenService(
            Optional<FirebaseAuth> firebaseAuth,
            FirebaseIdentityMapper firebaseIdentityMapper,
            @Value("${firebase.check-revoked:true}") boolean checkRevoked) {
        this.firebaseAuth = firebaseAuth;
        this.firebaseIdentityMapper = firebaseIdentityMapper;
        this.checkRevoked = checkRevoked;
    }

    @Override
    public FirebaseIdentityDTO verify(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw invalidToken();
        }

        FirebaseAuth auth = firebaseAuth.orElseThrow(
                () -> new ApplicationException(
                        ErrorCase.INVALID_OPERATION,
                        "La autenticación con Firebase no está configurada"
                )
        );

        FirebaseToken decodedToken;

        try {
            decodedToken = auth.verifyIdToken(idToken, checkRevoked);
        } catch (FirebaseAuthException | IllegalArgumentException exception) {
            throw invalidToken();
        }

        FirebaseIdentityDTO identity =
                firebaseIdentityMapper.toIdentity(decodedToken);

        if (!StringUtils.hasText(identity.email())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "La cuenta no tiene un correo asociado"
            );
        }

        if (!ALLOWED_PROVIDERS.contains(identity.provider())) {
            throw new ApplicationException(
                    ErrorCase.UNAUTHORIZED,
                    "Proveedor de autenticación no permitido"
            );
        }

        return identity;
    }

    private ApplicationException invalidToken() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Token de Firebase inválido"
        );
    }
}
