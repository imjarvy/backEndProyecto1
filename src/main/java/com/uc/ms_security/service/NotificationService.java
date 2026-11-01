package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.NotificationTwoFactorRequest;
import com.uc.ms_security.dto.auth.NotificationTwoFactorResponse;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RestClient.Builder restClientBuilder;

    @Value("${notification.base-url:http://localhost:8000}")
    private String baseUrl;

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
}
