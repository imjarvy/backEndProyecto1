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
import static org.mockito.Mockito.times;
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

    @Test
    void seedsUserRoleAndRolePermissionEndpointsForAdmin() throws Exception {
        User admin = new User();
        admin.setId(1L);
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

        DatabaseSeeder seeder = createSeeder("", false);
        seeder.seedDatabase().run();

        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/roles/{id}/detail-with-permissions",
                "GET",
                "Role"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/user-roles",
                "POST",
                "UserRole"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/user-roles",
                "GET",
                "UserRole"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/user-roles/users/{id}",
                "GET",
                "UserRole"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/user-roles/roles/{id}",
                "GET",
                "UserRole"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/user-roles/{id}/{id}",
                "DELETE",
                "UserRole"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/role-permissions",
                "POST",
                "RolePermission"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/role-permissions",
                "GET",
                "RolePermission"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/role-permissions/roles/{id}",
                "GET",
                "RolePermission"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/role-permissions/permissions/{id}",
                "GET",
                "RolePermission"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/role-permissions/{id}/{id}",
                "DELETE",
                "RolePermission"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/phases",
                "POST",
                "Phase"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/phases",
                "GET",
                "Phase"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/phases/{id}",
                "GET",
                "Phase"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/phases/{id}",
                "PATCH",
                "Phase"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/phases/{id}",
                "DELETE",
                "Phase"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/phases/{id}/groups",
                "POST",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/phases/{id}/groups",
                "GET",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/groups/{id}",
                "GET",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/groups/{id}",
                "PATCH",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/groups/{id}",
                "DELETE",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/groups/{id}/teams",
                "POST",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/groups/{id}/teams",
                "GET",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/groups/{id}/teams/{id}",
                "DELETE",
                "CompetitionGroup"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/matches",
                "GET",
                "Match"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/matches/{id}",
                "GET",
                "Match"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/matches",
                "POST",
                "Match"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/matches/{id}",
                "PATCH",
                "Match"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/matches/{id}",
                "DELETE",
                "Match"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/matches/{id}/result",
                "PATCH",
                "Match"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/teams",
                "POST",
                "Team"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/teams",
                "GET",
                "Team"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/teams/{id}",
                "GET",
                "Team"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/teams/{id}",
                "PATCH",
                "Team"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/teams/{id}",
                "DELETE",
                "Team"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/sports",
                "POST",
                "Sport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/sports",
                "GET",
                "Sport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/sports/{id}",
                "GET",
                "Sport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/sports/{id}",
                "PATCH",
                "Sport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/sports/{id}",
                "DELETE",
                "Sport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/team-sports",
                "POST",
                "TeamSport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/team-sports",
                "GET",
                "TeamSport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/team-sports/{id}",
                "GET",
                "TeamSport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/team-sports/{id}",
                "PATCH",
                "TeamSport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/team-sports/{id}",
                "DELETE",
                "TeamSport"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/registrations",
                "POST",
                "Registration"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/registrations",
                "GET",
                "Registration"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/registrations/{id}",
                "DELETE",
                "Registration"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/tournaments",
                "POST",
                "Tournament"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/tournaments",
                "GET",
                "Tournament"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/tournaments/{id}",
                "GET",
                "Tournament"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/tournaments/{id}/development",
                "GET",
                "Tournament"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/tournaments/{id}",
                "PATCH",
                "Tournament"
        );
        verify(permissionRepository).findByUrlAndMethodAndEntity(
                "/api/tournaments/{id}",
                "DELETE",
                "Tournament"
        );
        verify(permissionRepository, times(2)).findByUrlAndMethodAndEntity(
                "/api/profile-images/{id}",
                "GET",
                "ProfileImage"
        );
        verify(rolePermissionRepository, times(90))
                .existsByRoleIdAndPermissionId(1L, 1L);
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
