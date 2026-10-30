package com.uc.ms_security.controller;

import com.uc.ms_security.config.JwtConfig;
import com.uc.ms_security.config.SecurityConfig;
import com.uc.ms_security.authorization.DynamicAuthorizationManager;
import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.authorization.CookieBearerTokenResolver;
import com.uc.ms_security.service.AuthService;
import com.uc.ms_security.service.JwtService;
import com.uc.ms_security.service.PermissionService;
import com.uc.ms_security.service.OAuthService;
import com.uc.ms_security.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {AuthController.class, UserController.class},
        properties = "security.jwt.secret=test-only-signing-key-at-least-32-bytes"
)
@AutoConfigureMockMvc
@Import({
        JwtConfig.class,
        SecurityConfig.class,
        DynamicAuthorizationManager.class,
        CookieBearerTokenResolver.class,
        JwtService.class,
        AuthSecurityTests.WebSecurityTestConfiguration.class
})
class AuthSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OAuthService oauthService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PermissionService permissionService;

    @TestConfiguration
    @EnableWebSecurity
    static class WebSecurityTestConfiguration {
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        AuthResponseDTO token = new AuthResponseDTO(
                "signed-token",
                "Bearer",
                Instant.now().plusSeconds(900)
        );
        when(authService.login(any())).thenReturn(token);

        CsrfRequest csrf = getCsrfToken();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .content("""
                                {
                                  "email": "ana@example.com",
                                  "password": "ClaveSegura123!"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        "Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("AUTH_TOKEN=signed-token"),
                                org.hamcrest.Matchers.containsString("Path=/api"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )
                ));
    }

    @Test
    void protectedEndpointRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointDoesNotAcceptBearerHeaderInsteadOfCookie() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("ana@example.com");
        AuthResponseDTO token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("ana@example.com");
        when(userService.findAll()).thenReturn(List.of());
        when(permissionService.hasPermission(
                1L,
                "GET",
                "/api/users"
        )).thenReturn(true);

        AuthResponseDTO token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users")
                        .cookie(new Cookie("AUTH_TOKEN", token.accessToken())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .json("[]"));
    }

    @Test
    void protectedEndpointRejectsValidTokenWithoutPermission() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("ana@example.com");
        when(permissionService.hasPermission(
                1L,
                "GET",
                "/api/users"
        )).thenReturn(false);

        AuthResponseDTO token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users")
                        .cookie(new Cookie("AUTH_TOKEN", token.accessToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointRejectsModifiedToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("ana@example.com");
        String validToken = jwtService.generateToken(user).accessToken();
        String[] tokenParts = validToken.split("\\.");
        String signature = tokenParts[2];
        char replacement = signature.charAt(0) == 'A' ? 'B' : 'A';
        tokenParts[2] = replacement + signature.substring(1);
        String modifiedToken = String.join(".", tokenParts);

        mockMvc.perform(get("/api/users")
                        .cookie(new Cookie("AUTH_TOKEN", modifiedToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsExpiredToken() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject("1")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .build();
        String expiredToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        mockMvc.perform(get("/api/users")
                        .cookie(new Cookie("AUTH_TOKEN", expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfEndpointSetsCookieAndReturnsToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:4200"
                ))
                .andExpect(header().string(
                        "Access-Control-Allow-Credentials",
                        "true"
                ))
                .andExpect(header().string(
                        "Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("XSRF-TOKEN="),
                                org.hamcrest.Matchers.containsString("Path=/api"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )
                ))
                .andReturn();

        org.junit.jupiter.api.Assertions.assertFalse(readCsrfToken(result).isBlank());
        org.junit.jupiter.api.Assertions.assertNotNull(
                result.getResponse().getCookie("XSRF-TOKEN")
        );
    }

    @Test
    void currentSessionReturnsSafeIdentityFromCookieToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("ana@example.com");
        AuthResponseDTO token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/auth/session")
                        .cookie(new Cookie("AUTH_TOKEN", token.accessToken())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .json("""
                                {
                                  "id": 1,
                                  "email": "ana@example.com"
                                }
                                """));
    }

    @Test
    void logoutClearsAuthenticationCookieAndRequiresCsrf() throws Exception {
        CsrfRequest csrf = getCsrfToken();

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        "Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("AUTH_TOKEN="),
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("Path=/api"),
                                org.hamcrest.Matchers.containsString("HttpOnly")
                        )
                ));
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@example.com",
                                  "password": "ClaveSegura123!"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private CsrfRequest getCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        return new CsrfRequest(
                readCsrfToken(result),
                result.getResponse().getCookie("XSRF-TOKEN")
        );
    }

    private String readCsrfToken(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(
                matcher.find(),
                "La respuesta CSRF debe contener el token"
        );
        return matcher.group(1);
    }

    private record CsrfRequest(String token, Cookie cookie) {
    }
}
