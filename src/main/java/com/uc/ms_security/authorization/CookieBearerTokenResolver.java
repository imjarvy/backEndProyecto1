package com.uc.ms_security.authorization;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/2fa/verify",
            "/api/auth/oauth",
            "/api/auth/csrf",
            "/api/auth/logout",
            "/api/auth/password-recovery/request",
            "/api/auth/password-recovery/reset"
    );

    private final String cookieName;
    private final BearerTokenResolver headerBearerTokenResolver =
            new DefaultBearerTokenResolver();

    public CookieBearerTokenResolver(
            @Value("${security.jwt.cookie-name:AUTH_TOKEN}") String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (PUBLIC_AUTH_PATHS.contains(request.getRequestURI())) {
            return null;
        }

        String headerToken = headerBearerTokenResolver.resolve(request);
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
