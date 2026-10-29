package com.uc.ms_security.config;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseSeederTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PermissionRepository permissionRepository =
            mock(PermissionRepository.class);
    private final UserRoleRepository userRoleRepository =
            mock(UserRoleRepository.class);
    private final RolePermissionRepository rolePermissionRepository =
            mock(RolePermissionRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void explicitResetEncodesAndSavesExistingAdminPassword() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@uc.edu.co");
        admin.setPassword("invalid-stored-password");
        Role adminRole = role(1L);
        Role consultantRole = role(2L);
        Permission permission = new Permission();
        permission.setId(1L);

        when(userRepository.findByEmail("admin@uc.edu.co"))
                .thenReturn(Optional.of(admin));
        when(roleRepository.findByNameIgnoreCase("ADMIN"))
                .thenReturn(Optional.of(adminRole));
        when(roleRepository.findByNameIgnoreCase("CONSULTOR"))
                .thenReturn(Optional.of(consultantRole));
        when(permissionRepository.findByUrlAndMethodAndEntity(any(), any(), any()))
                .thenReturn(Optional.of(permission));
        when(userRoleRepository.existsByUserIdAndRoleId(1L, 1L))
                .thenReturn(true);
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(any(), any()))
                .thenReturn(true);
        when(passwordEncoder.matches("NuevaClave123!", admin.getPassword()))
                .thenReturn(false);
        when(passwordEncoder.encode("NuevaClave123!"))
                .thenReturn("$2a$12$encoded-password-hash");
        when(userRepository.save(admin)).thenReturn(admin);

        DatabaseSeeder seeder = createSeeder("NuevaClave123!", true);
        seeder.seedDatabase().run();

        assertEquals("$2a$12$encoded-password-hash", admin.getPassword());
        verify(passwordEncoder).encode("NuevaClave123!");
        verify(userRepository).save(admin);
    }

    @Test
    void explicitResetRequiresPasswordBeforeChangingExistingAdmin() {
        User admin = new User();
        admin.setPassword("existing-hash");
        when(userRepository.findByEmail("admin@uc.edu.co"))
                .thenReturn(Optional.of(admin));
        when(roleRepository.findByNameIgnoreCase("ADMIN"))
                .thenReturn(Optional.of(role(1L)));
        when(roleRepository.findByNameIgnoreCase("CONSULTOR"))
                .thenReturn(Optional.of(role(2L)));

        DatabaseSeeder seeder = createSeeder("", true);

        assertThrows(
                IllegalStateException.class,
                () -> seeder.seedDatabase().run()
        );
        verify(userRepository, never()).save(admin);
        verify(passwordEncoder, never()).encode(any());
    }

    private DatabaseSeeder createSeeder(String adminPassword, boolean reset) {
        return new DatabaseSeeder(
                userRepository,
                roleRepository,
                permissionRepository,
                userRoleRepository,
                rolePermissionRepository,
                passwordEncoder,
                adminPassword,
                reset
        );
    }

    private static Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }
}
