package com.uc.ms_security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import java.time.Duration;

public class SpaCsrfTokenRepository implements CsrfTokenRepository {

    private static final String COOKIE_NAME = "XSRF-TOKEN";
    private static final String COOKIE_PATH = "/api";

    private final boolean secure;
    private final CookieCsrfTokenRepository delegate =
            CookieCsrfTokenRepository.withHttpOnlyFalse();

    public SpaCsrfTokenRepository(boolean secure) {
        this.secure = secure;
        delegate.setCookieName(COOKIE_NAME);
        delegate.setHeaderName("X-XSRF-TOKEN");
        delegate.setCookieCustomizer(cookie -> cookie
                .path(COOKIE_PATH)
                .secure(secure));
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }

    @Override
    public void saveToken(
            CsrfToken token,
            HttpServletRequest request,
            HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie
                .from(COOKIE_NAME, token == null ? "" : token.getToken())
                .httpOnly(false)
                .secure(secure)
                .path(COOKIE_PATH)
                .sameSite("Lax");

        if (token == null) {
            cookie.maxAge(Duration.ZERO);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }
}
