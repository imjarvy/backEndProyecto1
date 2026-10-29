package com.uc.ms_security.controller;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.CsrfTokenResponseDTO;
import com.uc.ms_security.dto.auth.CurrentUserResponseDTO;
import com.uc.ms_security.dto.auth.LoginRequestDTO;
import com.uc.ms_security.service.AuthService;
import com.uc.ms_security.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;

    @Value("${security.jwt.cookie-name:AUTH_TOKEN}")
    private String cookieName;

    @Value("${security.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequestDTO dto) {
        AuthResponseDTO token = authService.login(dto);
        Duration maxAge = Duration.between(Instant.now(), token.expiresAt());
        ResponseCookie cookie = authCookie(token.accessToken(), maxAge);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/csrf")
    public CsrfTokenResponseDTO csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponseDTO(csrfToken.getToken());
    }

    @GetMapping("/session")
    public CurrentUserResponseDTO session(@AuthenticationPrincipal Jwt jwt) {
        return new CurrentUserResponseDTO(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("email")
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            sessionService.revokeAuthenticationSession(jwt.getId());
        }

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookie("", Duration.ZERO).toString()
                )
                .build();
    }

    private ResponseCookie authCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api")
                .sameSite("Lax")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }
}
