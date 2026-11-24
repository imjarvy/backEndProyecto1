package com.uc.ms_security.controller;

import com.uc.ms_security.service.AuthService;
import com.uc.ms_security.service.OAuthService;
import com.uc.ms_security.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AuthLogoutTests {

    private final SessionService sessions = mock(SessionService.class);
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final AuthController controller = new AuthController(
            mock(AuthService.class), sessions, mock(OAuthService.class), decoder
    );

    AuthLogoutTests() {
        ReflectionTestUtils.setField(controller, "cookieName", "AUTH_TOKEN");
    }

    @Test
    void revokesValidatedSessionAndClearsCookie() {
        Jwt jwt = mock(Jwt.class);
        when(decoder.decode("valid")).thenReturn(jwt);
        when(jwt.getId()).thenReturn("session-id");

        var response = controller.logout("valid");

        verify(sessions).revokeAuthenticationSession("session-id");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .anyMatch(cookie -> cookie.contains("Max-Age=0")));
    }

    @Test
    void clearsInvalidCookieWithoutRevokingSession() {
        when(decoder.decode("expired")).thenThrow(new JwtException("expired"));

        var response = controller.logout("expired");

        verifyNoInteractions(sessions);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .anyMatch(cookie -> cookie.contains("Max-Age=0")));
    }
}
