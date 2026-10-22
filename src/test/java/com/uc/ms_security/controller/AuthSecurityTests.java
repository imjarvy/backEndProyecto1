package com.uc.ms_security.controller;

import com.uc.ms_security.config.JwtConfig;
import com.uc.ms_security.config.SecurityConfig;
import com.uc.ms_security.authorization.DynamicAuthorizationManager;
import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.service.AuthService;
import com.uc.ms_security.service.JwtService;
import com.uc.ms_security.service.PermissionService;
import com.uc.ms_security.service.UserService;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@example.com",
                                  "password": "ClaveSegura123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "accessToken": "signed-token",
                          "tokenType": "Bearer"
                        }
                        """));
    }

    @Test
    void protectedEndpointRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
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
                        .header("Authorization", "Bearer " + token.accessToken()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
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
                        .header("Authorization", "Bearer " + token.accessToken()))
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
                        .header("Authorization", "Bearer " + modifiedToken))
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
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }
}
