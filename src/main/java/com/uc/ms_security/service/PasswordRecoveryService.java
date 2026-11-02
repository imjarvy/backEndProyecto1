package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.PasswordRecoveryRequestDTO;
import com.uc.ms_security.dto.auth.PasswordResetRequestDTO;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.PasswordRecoveryToken;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.PasswordRecoveryTokenRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final Logger logger =
            LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final String REQUEST_MESSAGE =
            "Solicitud recibida. Si la cuenta es compatible, el sistema "
                    + "intentará enviar instrucciones de recuperación.";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordRecoveryTokenService tokenService;
    private final PasswordRecoveryTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final NotificationService notificationService;

    @Value("${password-recovery.reset-url:http://localhost:4200/authentication/reset-password}")
    private String resetUrl;

    public String requestRecovery(PasswordRecoveryRequestDTO request) {
        String token = generateToken();
        String tokenHash = hashToken(token);
        Optional<PasswordRecoveryToken> recoveryToken =
                tokenService.issueForLocalUser(request.email(), tokenHash);

        if (recoveryToken.isPresent()) {
            logger.info(
                    "Password recovery token issued for eligible local account"
            );
            String recoveryUrl = UriComponentsBuilder
                    .fromUriString(resetUrl)
                    .queryParam("token", token)
                    .build()
                    .encode()
                    .toUriString();
            try {
                notificationService.requestPasswordRecoveryEmail(
                        request.email(),
                        recoveryUrl
                );
                logger.info(
                        "Password recovery request accepted by notification service"
                );
            } catch (ApplicationException exception) {
                tokenService.invalidateByHash(tokenHash);
                logger.error(
                        "Password recovery email delivery failed; "
                                + "the recovery token was invalidated ({})",
                        exception.getErrorCase()
                );
            }
        } else {
            logger.info(
                    "Password recovery email not attempted: "
                            + "no eligible local account"
            );
        }

        return REQUEST_MESSAGE;
    }

    @Transactional
    public void resetPassword(PasswordResetRequestDTO request) {
        String tokenHash = hashToken(request.token());
        PasswordRecoveryToken recoveryToken =
                tokenRepository.findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(this::invalidToken);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (recoveryToken.getUsedAt() != null
                || !recoveryToken.getExpiresAt().isAfter(now)
                || recoveryToken.getUser().getAuthProvider()
                != AuthProvider.LOCAL_PASSWORD) {
            throw invalidToken();
        }

        recoveryToken.setUsedAt(now);
        recoveryToken.getUser().setPassword(
                passwordEncoder.encode(request.newPassword())
        );
        userRepository.save(recoveryToken.getUser());
        sessionService.revokeActiveSessionsByUserId(
                recoveryToken.getUser().getId()
        );
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El token no es válido, expiró o ya fue utilizado"
        );
    }
}
