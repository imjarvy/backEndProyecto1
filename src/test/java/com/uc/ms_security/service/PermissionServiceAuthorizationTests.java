package com.uc.ms_security.service;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.mapper.PermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionServiceAuthorizationTests {

    private final PermissionRepository permissionRepository =
            mock(PermissionRepository.class);
    private final RolePermissionRepository rolePermissionRepository =
            mock(RolePermissionRepository.class);

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(
                permissionRepository,
                rolePermissionRepository,
                new PermissionMapper()
        );
    }

    @Test
    void hasPermissionNormalizesNumericIdAndUppercasesMethod() {
        when(permissionRepository.findByUserIdAndMethodAndUrl(
                15L,
                "DELETE",
                "/api/users/{id}"
        )).thenReturn(List.of(new Permission()));

        assertTrue(permissionService.hasPermission(
                15L,
                "delete",
                "/api/users/15"
        ));
        verify(permissionRepository).findByUserIdAndMethodAndUrl(
                15L,
                "DELETE",
                "/api/users/{id}"
        );
    }

    @Test
    void hasPermissionNormalizesMongoObjectId() {
        when(permissionRepository.findByUserIdAndMethodAndUrl(
                15L,
                "GET",
                "/api/users/{id}/profile"
        )).thenReturn(List.of(new Permission()));

        assertTrue(permissionService.hasPermission(
                15L,
                "GET",
                "/api/users/507f1f77bcf86cd799439011/profile"
        ));
    }

    @Test
    void hasPermissionNormalizesIdsForUserRoleAndRolePermissionEndpoints() {
        when(permissionRepository.findByUserIdAndMethodAndUrl(
                15L,
                "GET",
                "/api/user-roles/users/{id}"
        )).thenReturn(List.of(new Permission()));
        when(permissionRepository.findByUserIdAndMethodAndUrl(
                15L,
                "GET",
                "/api/role-permissions/roles/{id}"
        )).thenReturn(List.of(new Permission()));

        assertTrue(permissionService.hasPermission(
                15L,
                "GET",
                "/api/user-roles/users/27"
        ));
        assertTrue(permissionService.hasPermission(
                15L,
                "GET",
                "/api/role-permissions/roles/3"
        ));
    }

    @Test
    void hasPermissionDoesNotNormalizeUuid() {
        when(permissionRepository.findByUserIdAndMethodAndUrl(
                15L,
                "GET",
                "/api/users/507f1f77-bcf8-6cd7-9943-9011"
        )).thenReturn(List.of());

        assertFalse(permissionService.hasPermission(
                15L,
                "GET",
                "/api/users/507f1f77-bcf8-6cd7-9943-9011"
        ));
        verify(permissionRepository).findByUserIdAndMethodAndUrl(
                15L,
                "GET",
                "/api/users/507f1f77-bcf8-6cd7-9943-9011"
        );
    }
}