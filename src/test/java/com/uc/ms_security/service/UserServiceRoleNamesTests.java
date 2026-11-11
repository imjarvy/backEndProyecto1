package com.uc.ms_security.service;

import com.uc.ms_security.dto.user.UserRoleNamesResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.uc.ms_security.repository.PasswordRecoveryTokenRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceRoleNamesTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(
        userRepository,
        mock(PasswordRecoveryTokenRepository.class),
        mock(UserMapper.class),
        mock(PasswordEncoder.class)
);

    @Test
    void returnsOnlyPersistedRoleNamesForRequestedAuthenticatedUser() {
        User user = userWithRoles("CONSULTOR");
        when(userRepository.findWithRolesById(17L))
                .thenReturn(Optional.of(user));

        UserRoleNamesResponseDTO response =
                userService.findRoleNamesByUserId(17L);

        assertEquals(List.of("CONSULTOR"), response.roles());
        verify(userRepository).findWithRolesById(17L);
    }

    @Test
    void returnsEmptyRoleListWhenUserHasNoRoles() {
        User user = userWithRoles();
        when(userRepository.findWithRolesById(42L))
                .thenReturn(Optional.of(user));

        UserRoleNamesResponseDTO response =
                userService.findRoleNamesByUserId(42L);

        assertEquals(List.of(), response.roles());
    }

    private static User userWithRoles(String... roleNames) {
        User user = new User();
        List<UserRole> userRoles = java.util.Arrays.stream(roleNames)
                .map(UserServiceRoleNamesTests::userRole)
                .toList();
        user.setUserRoles(userRoles);
        return user;
    }

    private static UserRole userRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);

        UserRole userRole = new UserRole();
        userRole.setRole(role);
        return userRole;
    }
}
