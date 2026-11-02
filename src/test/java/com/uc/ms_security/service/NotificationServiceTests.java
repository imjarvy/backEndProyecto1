package com.uc.ms_security.service;

import com.uc.ms_security.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpStatus;

class NotificationServiceTests {

    @TempDir
    Path tempDirectory;

    @Test
    void readsSharedTokenFileAndSendsPasswordRecoveryRequest()
            throws Exception {
        String sharedSecret = "shared-local-test-secret";
        Path secretFile = tempDirectory.resolve(".notification-service-token");
        Files.writeString(secretFile, sharedSecret, StandardCharsets.UTF_8);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();
        NotificationService service = service(builder, secretFile);

        server.expect(requestTo(
                        "http://localhost:8000/internal/email/password-recovery"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Service-Token", sharedSecret))
                .andExpect(content().json(
                        """
                                {
                                  "to":"ana@example.com",
                                  "recoveryUrl":"http://localhost:4200/reset?token=abc"
                                }
                                """
                ))
                .andRespond(withSuccess());

        service.requestPasswordRecoveryEmail(
                "ana@example.com",
                "http://localhost:4200/reset?token=abc"
        );

        server.verify();
    }

    @Test
    void reportsGenericNotificationErrorWhenFastApiRejectsTheRequest()
            throws Exception {
        Path secretFile = tempDirectory.resolve(".notification-service-token");
        Files.writeString(secretFile, "shared-local-test-secret");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();
        NotificationService service = service(builder, secretFile);

        server.expect(requestTo(
                        "http://localhost:8000/internal/email/password-recovery"
                ))
                .andExpect(header(
                        "X-Service-Token",
                        "shared-local-test-secret"
                ))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("sensitive upstream response"));

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> service.requestPasswordRecoveryEmail(
                        "ana@example.com",
                        "http://localhost:4200/reset?token=abc"
                )
        );

        assertEquals(
                "El servicio de notificaciones no está disponible",
                exception.getMessage()
        );
        server.verify();
    }

    @Test
    void missingSharedSecretFailsBeforeCallingFastApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();
        Path missingSecret = tempDirectory.resolve("missing-secret");
        NotificationService service = service(builder, missingSecret);

        assertThrows(
                ApplicationException.class,
                () -> service.requestPasswordRecoveryEmail(
                        "ana@example.com",
                        "http://localhost:4200/reset?token=abc"
                )
        );

        server.verify();
    }

    private NotificationService service(
            RestClient.Builder builder,
            Path secretFile) {
        NotificationService service = new NotificationService(builder);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(service, "serviceToken", "");
        ReflectionTestUtils.setField(
                service,
                "serviceTokenFile",
                secretFile.toString()
        );
        return service;
    }
}