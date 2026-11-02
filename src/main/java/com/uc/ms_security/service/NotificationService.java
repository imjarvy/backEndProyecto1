package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.NotificationTwoFactorRequest;
import com.uc.ms_security.dto.auth.NotificationTwoFactorResponse;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationService.class);
    private final RestClient.Builder restClientBuilder;

    @Value("${notification.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${notification.service-token:}")
    private String serviceToken;

    @Value("${notification.service-token-file:../ms_notificaciones/Backend_ms_notificaciones/.notification-service-token}")
    private String serviceTokenFile;

    public void requestPasswordRecoveryEmail(
            String email,
            String recoveryUrl) {
        String configuredToken = resolveServiceToken();
        if (!StringUtils.hasText(configuredToken)) {
            logger.error(
                    "Password recovery notification was not sent: "
                            + "shared service credential is unavailable"
            );
            throw notificationUnavailable();
        }

        try {
            logger.info("Calling notification service for password recovery");
            HttpStatusCode responseStatus = client()
                    .post()
                    .uri("/internal/email/password-recovery")
                    .header("X-Service-Token", configuredToken)
                    .body(new PasswordRecoveryEmailRequest(email, recoveryUrl))
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            logger.info(
                    "Notification service responded to password recovery "
                            + "request with HTTP {}",
                    responseStatus.value()
            );
        } catch (RestClientResponseException exception) {
            logger.error(
                    "Notification service rejected/failed password recovery "
                            + "request with HTTP {}",
                    exception.getStatusCode().value()
            );
            throw notificationUnavailable(exception);
        } catch (RestClientException exception) {
            logger.error(
                    "Notification service transport failed for password "
                            + "recovery request ({})",
                    exception.getClass().getSimpleName()
            );
            throw notificationUnavailable(exception);
        }
    }

    private String resolveServiceToken() {
        if (StringUtils.hasText(serviceToken)) {
            return serviceToken;
        }

        try {
            return Files.readString(
                    Path.of(serviceTokenFile),
                    StandardCharsets.UTF_8
            ).trim();
        } catch (IOException | InvalidPathException exception) {
            return "";
        }
    }

    public NotificationTwoFactorResponse requestTwoFactorCode(String email) {
        try {
            NotificationTwoFactorResponse response = client()
                    .post()
                    .uri("/2fa/request")
                    .body(new NotificationTwoFactorRequest(email))
                    .retrieve()
                    .body(NotificationTwoFactorResponse.class);

            if (response == null || response.challengeId() == null
                    || response.challengeId().isBlank()
                    || response.expiresIn() <= 0) {
                throw notificationUnavailable();
            }
            return response;
        } catch (RestClientException exception) {
            throw notificationUnavailable(exception);
        }
    }

    public boolean verifyTwoFactor(
            String email,
            String challengeId,
            String code) {
        try {
            HttpStatusCode status = client()
                    .post()
                    .uri("/2fa/verify")
                    .body(new VerifyTwoFactorRequest(
                            email,
                            challengeId,
                            code
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            return status.is2xxSuccessful();
        } catch (RestClientException exception) {
            if (exception instanceof org.springframework.web.client
                    .HttpClientErrorException.BadRequest) {
                return false;
            }
            throw notificationUnavailable(exception);
        }
    }

    private RestClient client() {
        return restClientBuilder.baseUrl(baseUrl).build();
    }

    private ApplicationException notificationUnavailable() {
        return notificationUnavailable(null);
    }

    private ApplicationException notificationUnavailable(Exception cause) {
        ApplicationException exception = new ApplicationException(
                ErrorCase.INVALID_OPERATION,
                "El servicio de notificaciones no está disponible"
        );
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    private record VerifyTwoFactorRequest(
            String email,
            @JsonProperty("challenge_id")
            String challengeId,
            String code) {
    }

    private record PasswordRecoveryEmailRequest(
            String to,
            String recoveryUrl) {
    }
}
