package com.uc.ms_security.controller;

import com.uc.ms_security.authorization.CookieBearerTokenResolver;
import com.uc.ms_security.authorization.DynamicAuthorizationManager;
import com.uc.ms_security.config.SecurityConfig;
import com.uc.ms_security.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionValidationController.class)
@Import({
        SecurityConfig.class,
        DynamicAuthorizationManager.class,
        CookieBearerTokenResolver.class
})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
class PermissionValidationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/internal/security/permissions-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"/cinema/movies","method":"POST"}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(permissionService);
    }

    @Test
    void validTokenReturnsPermissionDecision() throws Exception {
        when(jwtDecoder.decode("valid-token"))
                .thenReturn(jwt("valid-token", "17"));
        when(permissionService.hasPermission(
                17L,
                "post",
                "/cinema/movies"
        )).thenReturn(true);

        mockMvc.perform(post("/api/internal/security/permissions-validation")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"/cinema/movies","method":"post"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"allowed\":true}", true));

        verify(permissionService).hasPermission(
                17L,
                "post",
                "/cinema/movies"
        );
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {
        when(jwtDecoder.decode("valid-token"))
                .thenReturn(jwt("valid-token", "17"));

        mockMvc.perform(post("/api/internal/security/permissions-validation")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"","method":""}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(permissionService);
    }

    private static Jwt jwt(String token, String subject) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
