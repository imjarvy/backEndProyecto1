package com.uc.ms_security.service;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.PermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionServiceRolePermissionTests {

    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
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
    void deleteRejectsPermissionAssignedToRole() {
        Permission permission = new Permission();
        permission.setId(1L);
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.existsByPermissionId(1L)).thenReturn(true);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> permissionService.delete(1L)
        );

        assertEquals(ErrorCase.INVALID_OPERATION, exception.getErrorCase());
        verify(permissionRepository, never()).delete(permission);
    }
}
