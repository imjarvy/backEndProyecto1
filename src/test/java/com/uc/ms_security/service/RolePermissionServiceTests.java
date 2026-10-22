package com.uc.ms_security.service;

import com.uc.ms_security.dto.permission.AssignPermissionRequestDTO;
import com.uc.ms_security.dto.rolepermission.RolePermissionResponseDTO;
import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.RolePermission;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.PermissionMapper;
import com.uc.ms_security.mapper.RolePermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import com.uc.ms_security.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RolePermissionServiceTests {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final RolePermissionRepository rolePermissionRepository =
            mock(RolePermissionRepository.class);
    private final RolePermissionMapper rolePermissionMapper =
            new RolePermissionMapper(new PermissionMapper());

    private RolePermissionService rolePermissionService;

    @BeforeEach
    void setUp() {
        rolePermissionService = new RolePermissionService(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                rolePermissionMapper
        );
    }

    @Test
    void assignCreatesRolePermission() {
        AssignPermissionRequestDTO dto = new AssignPermissionRequestDTO();
        dto.setRoleId(1L);
        dto.setPermissionId(2L);

        Role role = new Role();
        role.setId(1L);
        Permission permission = new Permission();
        permission.setId(2L);
        permission.setUrl("/api/users");
        permission.setMethod("GET");
        permission.setModel("User");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(1L, 2L))
                .thenReturn(false);
        when(rolePermissionRepository.save(org.mockito.ArgumentMatchers.any(RolePermission.class)))
                .thenAnswer(invocation -> {
                    RolePermission saved = invocation.getArgument(0);
                    saved.setId(3L);
                    return saved;
                });

        RolePermissionResponseDTO response = rolePermissionService.assign(dto);

        assertEquals(3L, response.getId());
        assertEquals(1L, response.getRoleId());
        assertEquals(2L, response.getPermission().getId());
        verify(rolePermissionRepository).save(
                org.mockito.ArgumentMatchers.any(RolePermission.class)
        );
    }

    @Test
    void assignRejectsDuplicateRolePermission() {
        AssignPermissionRequestDTO dto = new AssignPermissionRequestDTO();
        dto.setRoleId(1L);
        dto.setPermissionId(2L);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(new Role()));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(new Permission()));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(1L, 2L))
                .thenReturn(true);

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> rolePermissionService.assign(dto)
        );

        assertEquals(ErrorCase.ALREADY_EXISTS, exception.getErrorCase());
        verify(rolePermissionRepository, never()).save(
                org.mockito.ArgumentMatchers.any(RolePermission.class)
        );
    }

    @Test
    void assignReturnsNotFoundWhenRoleDoesNotExist() {
        AssignPermissionRequestDTO dto = new AssignPermissionRequestDTO();
        dto.setRoleId(1L);
        dto.setPermissionId(2L);
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> rolePermissionService.assign(dto)
        );

        assertEquals(ErrorCase.NOT_FOUND, exception.getErrorCase());
        verify(permissionRepository, never()).findById(2L);
    }
}
