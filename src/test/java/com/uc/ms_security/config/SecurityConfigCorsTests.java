package com.uc.ms_security.config;

import com.uc.ms_security.authorization.CookieBearerTokenResolver;
import com.uc.ms_security.authorization.DynamicAuthorizationManager;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTests {

    @Test
    void allowsLocalAngularOriginsAndCsrfPreflightHeaders() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(DynamicAuthorizationManager.class),
                mock(CookieBearerTokenResolver.class)
        );
        ReflectionTestUtils.setField(
                securityConfig,
                "allowedOrigins",
                "http://localhost:4200,http://127.0.0.1:4200"
        );

        CorsConfiguration configuration = configurationFor(
                securityConfig,
                "http://127.0.0.1:4200"
        );

        assertEquals(
                "http://127.0.0.1:4200",
                configuration.checkOrigin("http://127.0.0.1:4200")
        );
        assertEquals(
                "http://localhost:4200",
                configuration.checkOrigin("http://localhost:4200")
        );
        assertTrue(configuration
                .getAllowedHeaders()
                .contains("X-XSRF-TOKEN"));
        assertTrue(configuration
                .getAllowedMethods()
                .contains("POST"));
        assertTrue(configuration.getAllowCredentials());
    }

    @Test
    void doesNotAllowUnconfiguredFrontendOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(DynamicAuthorizationManager.class),
                mock(CookieBearerTokenResolver.class)
        );
        ReflectionTestUtils.setField(
                securityConfig,
                "allowedOrigins",
                "http://localhost:4200,http://127.0.0.1:4200"
        );

        CorsConfiguration configuration = configurationFor(
                securityConfig,
                "http://malicious.example"
        );

        assertEquals(
                null,
                configuration.checkOrigin("http://malicious.example")
        );
    }

    private CorsConfiguration configurationFor(
            SecurityConfig securityConfig,
            String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/password-recovery/request");
        request.addHeader("Origin", origin);

        CorsConfiguration configuration =
                securityConfig.corsConfigurationSource()
                        .getCorsConfiguration(request);
        assertNotNull(configuration);
        return configuration;
    }
}
