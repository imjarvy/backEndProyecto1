package com.uc.ms_security.controller;

import com.uc.ms_security.authorization.CookieBearerTokenResolver;
import com.uc.ms_security.authorization.DynamicAuthorizationManager;
import com.uc.ms_security.config.SecurityConfig;
import com.uc.ms_security.dto.user.UserRoleNamesResponseDTO;
import com.uc.ms_security.service.PermissionService;
import com.uc.ms_security.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
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
class UserRolesEndpointSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me/roles"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService, permissionService);
    }

    @Test
    void authenticatedRegularUserReceivesOnlyTheirRoleNames()
            throws Exception {
        when(jwtDecoder.decode("consultant-token"))
                .thenReturn(jwt("consultant-token", "17"));
        when(userService.findRoleNamesByUserId(17L))
                .thenReturn(new UserRoleNamesResponseDTO(
                        List.of("CONSULTOR")
                ));

        mockMvc.perform(get("/api/users/me/roles")
                        .cookie(new Cookie("AUTH_TOKEN", "consultant-token")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"roles\":[\"CONSULTOR\"]}",
                        true
                ));

        verify(userService).findRoleNamesByUserId(17L);
        verifyNoInteractions(permissionService);
    }

    @Test
    void authenticatedAdminReceivesOnlyTheirRoleNames() throws Exception {
        when(jwtDecoder.decode("admin-token"))
                .thenReturn(jwt("admin-token", "42"));
        when(userService.findRoleNamesByUserId(42L))
                .thenReturn(new UserRoleNamesResponseDTO(List.of("ADMIN")));

        mockMvc.perform(get("/api/users/me/roles")
                        .cookie(new Cookie("AUTH_TOKEN", "admin-token")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"roles\":[\"ADMIN\"]}",
                        true
                ));

        verify(userService).findRoleNamesByUserId(42L);
        verifyNoInteractions(permissionService);
    }

    @Test
    void authenticatedUserWithoutRolesReceivesAnEmptyList()
            throws Exception {
        when(jwtDecoder.decode("no-roles-token"))
                .thenReturn(jwt("no-roles-token", "56"));
        when(userService.findRoleNamesByUserId(56L))
                .thenReturn(new UserRoleNamesResponseDTO(List.of()));

        mockMvc.perform(get("/api/users/me/roles")
                        .cookie(new Cookie("AUTH_TOKEN", "no-roles-token")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"roles\":[]}", true));

        verify(userService).findRoleNamesByUserId(56L);
        verifyNoInteractions(permissionService);
    }

    @Test
    void clientSuppliedIdsCannotChangeWhichUsersRolesAreReturned()
            throws Exception {
        when(jwtDecoder.decode("consultant-token"))
                .thenReturn(jwt("consultant-token", "17"));
        when(userService.findRoleNamesByUserId(17L))
                .thenReturn(new UserRoleNamesResponseDTO(
                        List.of("CONSULTOR")
                ));

        mockMvc.perform(get("/api/users/me/roles")
                        .param("userId", "42")
                        .param("id", "42")
                        .cookie(new Cookie("AUTH_TOKEN", "consultant-token")))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"roles\":[\"CONSULTOR\"]}",
                        true
                ));

        verify(userService).findRoleNamesByUserId(17L);
        verifyNoInteractions(permissionService);
    }

    @Test
    void userIdPathDoesNotAllowBypassingDynamicPermissions()
            throws Exception {
        when(jwtDecoder.decode("consultant-token"))
                .thenReturn(jwt("consultant-token", "17"));

        mockMvc.perform(get("/api/users/42/roles")
                        .cookie(new Cookie("AUTH_TOKEN", "consultant-token")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    private static Jwt jwt(String token, String subject) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
