package com.uc.ms_security.config;

import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.RolePermission;
import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
public class DatabaseSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private static final PermissionSeed[] ADMIN_PERMISSIONS = {
            new PermissionSeed("GET", "/api/users", "User"),
            new PermissionSeed("POST", "/api/users", "User"),
            new PermissionSeed("GET", "/api/users/{id}", "User"),
            new PermissionSeed("PUT", "/api/users/{id}", "User"),
            new PermissionSeed("DELETE", "/api/users/{id}", "User"),
            new PermissionSeed("GET", "/api/users/{id}/profile", "Profile"),
            new PermissionSeed("POST", "/api/users/{id}/profile", "Profile"),
            new PermissionSeed("PUT", "/api/users/{id}/profile", "Profile"),
            new PermissionSeed("DELETE", "/api/users/{id}/profile", "Profile"),
            new PermissionSeed("GET", "/api/profile-images/{id}", "ProfileImage"),
            new PermissionSeed("GET", "/api/roles", "Role"),
            new PermissionSeed("POST", "/api/roles", "Role"),
            new PermissionSeed("GET", "/api/roles/{id}", "Role"),
            new PermissionSeed(
                    "GET",
                    "/api/roles/{id}/detail-with-permissions",
                    "Role"
            ),
            new PermissionSeed("PUT", "/api/roles/{id}", "Role"),
            new PermissionSeed("DELETE", "/api/roles/{id}", "Role"),
            new PermissionSeed("GET", "/api/permissions", "Permission"),
            new PermissionSeed("POST", "/api/permissions", "Permission"),
            new PermissionSeed("GET", "/api/permissions/{id}", "Permission"),
            new PermissionSeed("PUT", "/api/permissions/{id}", "Permission"),
            new PermissionSeed("DELETE", "/api/permissions/{id}", "Permission"),
            new PermissionSeed("POST", "/api/user-roles", "UserRole"),
            new PermissionSeed("GET", "/api/user-roles", "UserRole"),
            new PermissionSeed("GET", "/api/user-roles/users/{id}", "UserRole"),
            new PermissionSeed("GET", "/api/user-roles/roles/{id}", "UserRole"),
            new PermissionSeed(
                    "DELETE",
                    "/api/user-roles/{id}/{id}",
                    "UserRole"
            ),
            new PermissionSeed("POST", "/api/role-permissions", "RolePermission"),
            new PermissionSeed("GET", "/api/role-permissions", "RolePermission"),
            new PermissionSeed(
                    "GET",
                    "/api/role-permissions/roles/{id}",
                    "RolePermission"
            ),
            new PermissionSeed(
                    "GET",
                    "/api/role-permissions/permissions/{id}",
                    "RolePermission"
            ),
            new PermissionSeed(
                    "DELETE",
                    "/api/role-permissions/{id}/{id}",
                    "RolePermission"
            ),
            new PermissionSeed("POST", "/api/phases", "Phase"),
            new PermissionSeed("GET", "/api/phases", "Phase"),
            new PermissionSeed("GET", "/api/phases/{id}", "Phase"),
            new PermissionSeed("PATCH", "/api/phases/{id}", "Phase"),
            new PermissionSeed("DELETE", "/api/phases/{id}", "Phase"),
            new PermissionSeed(
                    "POST",
                    "/api/phases/{id}/groups",
                    "CompetitionGroup"
            ),
            new PermissionSeed(
                    "GET",
                    "/api/phases/{id}/groups",
                    "CompetitionGroup"
            ),
            new PermissionSeed(
                    "GET",
                    "/api/groups/{id}",
                    "CompetitionGroup"
            ),
            new PermissionSeed(
                    "PATCH",
                    "/api/groups/{id}",
                    "CompetitionGroup"
            ),
            new PermissionSeed(
                    "DELETE",
                    "/api/groups/{id}",
                    "CompetitionGroup"
            ),
            new PermissionSeed(
                    "POST",
                    "/api/groups/{id}/teams",
                    "CompetitionGroup"
            ),
            new PermissionSeed(
                    "GET",
                    "/api/groups/{id}/teams",
                    "CompetitionGroup"
            ),
            new PermissionSeed(
                    "DELETE",
                    "/api/groups/{id}/teams/{id}",
                    "CompetitionGroup"
            ),
            new PermissionSeed("GET", "/api/matches", "Match"),
            new PermissionSeed("GET", "/api/matches/{id}", "Match"),
            new PermissionSeed("POST", "/api/matches", "Match"),
            new PermissionSeed("PATCH", "/api/matches/{id}", "Match"),
            new PermissionSeed("PATCH", "/api/matches/{id}/result", "Match"),
            new PermissionSeed("POST", "/api/teams", "Team"),
            new PermissionSeed("GET", "/api/teams", "Team"),
            new PermissionSeed("GET", "/api/teams/{id}", "Team"),
            new PermissionSeed("PATCH", "/api/teams/{id}", "Team"),
            new PermissionSeed("DELETE", "/api/teams/{id}", "Team"),
            new PermissionSeed("POST", "/api/sports", "Sport"),
            new PermissionSeed("GET", "/api/sports", "Sport"),
            new PermissionSeed("GET", "/api/sports/{id}", "Sport"),
            new PermissionSeed("PATCH", "/api/sports/{id}", "Sport"),
            new PermissionSeed("DELETE", "/api/sports/{id}", "Sport"),
            new PermissionSeed("POST", "/api/team-sports", "TeamSport"),
            new PermissionSeed("GET", "/api/team-sports", "TeamSport"),
            new PermissionSeed("GET", "/api/team-sports/{id}", "TeamSport"),
            new PermissionSeed("PATCH", "/api/team-sports/{id}", "TeamSport"),
            new PermissionSeed("DELETE", "/api/team-sports/{id}", "TeamSport"),
            new PermissionSeed("POST", "/api/registrations", "Registration"),
            new PermissionSeed("GET", "/api/registrations", "Registration"),
            new PermissionSeed("DELETE", "/api/registrations/{id}", "Registration"),
            new PermissionSeed("POST", "/api/tournaments", "Tournament"),
            new PermissionSeed("GET", "/api/tournaments", "Tournament"),
            new PermissionSeed("GET", "/api/tournaments/{id}", "Tournament"),
            new PermissionSeed(
                    "GET",
                    "/api/tournaments/{id}/development",
                    "Tournament"
            ),
            new PermissionSeed("PATCH", "/api/tournaments/{id}", "Tournament"),
            new PermissionSeed("DELETE", "/api/tournaments/{id}", "Tournament"),
            new PermissionSeed("DELETE", "/api/matches/{id}", "Match"),
            new PermissionSeed("POST", "/api/players", "Player"),
            new PermissionSeed("GET", "/api/players", "Player"),
            new PermissionSeed("GET", "/api/players/{id}", "Player"),
            new PermissionSeed("GET", "/api/players/{id}/profile", "Player"),
            new PermissionSeed("PATCH", "/api/players/{id}", "Player"),
            new PermissionSeed("DELETE", "/api/players/{id}", "Player"),
            new PermissionSeed("POST", "/api/skills", "Skill"),
            new PermissionSeed("GET", "/api/skills", "Skill"),
            new PermissionSeed("GET", "/api/skills/{id}", "Skill"),
            new PermissionSeed("PATCH", "/api/skills/{id}", "Skill"),
            new PermissionSeed("DELETE", "/api/skills/{id}", "Skill"),
            new PermissionSeed(
                    "POST",
                    "/api/players/{id}/skills",
                    "PlayerSkill"
            ),
            new PermissionSeed(
                    "GET",
                    "/api/players/{id}/skills",
                    "PlayerSkill"
            ),
            new PermissionSeed(
                    "PATCH",
                    "/api/players/{id}/skills/{id}",
                    "PlayerSkill"
            ),
            new PermissionSeed(
                    "DELETE",
                    "/api/players/{id}/skills/{id}",
                    "PlayerSkill"
            ),
            new PermissionSeed("GET", "/api/skills/{id}/players", "PlayerSkill"),
            new PermissionSeed("POST", "/api/teams/{id}/players", "TeamPlayer"),
            new PermissionSeed(
                    "PATCH",
                    "/api/teams/{id}/players/{id}/retirement",
                    "TeamPlayer"
            ),
            new PermissionSeed("GET", "/api/teams/{id}/players", "TeamPlayer"),
            new PermissionSeed("GET", "/api/players/{id}/teams", "TeamPlayer"),
            new PermissionSeed("DELETE", "/api/player-teams/{id}", "TeamPlayer"),
            new PermissionSeed(
                    "POST",
                    "/api/matches/{id}/annotations",
                    "Annotation"
            ),
            new PermissionSeed(
                    "GET",
                    "/api/matches/{id}/annotations",
                    "Annotation"
            ),
            new PermissionSeed("PATCH", "/api/annotations/{id}", "Annotation"),
            new PermissionSeed("DELETE", "/api/annotations/{id}", "Annotation"),
    };

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final boolean resetAdminPassword;

    public DatabaseSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${SEED_ADMIN_PASSWORD:}") String adminPassword,
            @Value("${RESET_ADMIN_PASSWORD:false}") boolean resetAdminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.resetAdminPassword = resetAdminPassword;
    }

    @Bean
    CommandLineRunner seedDatabase() {
        return arguments -> {
            Role adminRole = findOrCreateRole("ADMIN", "Administrador");
            Role consultantRole = findOrCreateRole(
                    "CONSULTOR",
                    "Consulta de usuarios"
            );

            User adminUser = findOrCreateAdminUser();
            if (adminUser != null) {
                assignRole(adminUser, adminRole);
            }

            for (PermissionSeed seed : ADMIN_PERMISSIONS) {
                assignPermission(adminRole, findOrCreatePermission(seed));
            }

            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed("GET", "/api/users", "User")
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed("GET", "/api/users/{id}", "User")
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/users/{id}/profile",
                                    "Profile"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "POST",
                                    "/api/users/{id}/profile",
                                    "Profile"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/profile-images/{id}",
                                    "ProfileImage"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed("GET", "/api/players", "Player")
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed("GET", "/api/players/{id}", "Player")
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/players/{id}/profile",
                                    "Player"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed("GET", "/api/skills", "Skill")
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed("GET", "/api/skills/{id}", "Skill")
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/players/{id}/skills",
                                    "PlayerSkill"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/skills/{id}/players",
                                    "PlayerSkill"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/teams/{id}/players",
                                    "TeamPlayer"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/players/{id}/teams",
                                    "TeamPlayer"
                            )
                    )
            );
            assignPermission(
                    consultantRole,
                    findOrCreatePermission(
                            new PermissionSeed(
                                    "GET",
                                    "/api/matches/{id}/annotations",
                                    "Annotation"
                            )
                    )
            );
        };
    }

    private Role findOrCreateRole(String name, String description) {
        return roleRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private User findOrCreateAdminUser() {
        Optional<User> existingUser = userRepository.findByEmail("admin@uc.edu.co");

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!resetAdminPassword) {
                return user;
            }
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "SEED_ADMIN_PASSWORD es obligatorio cuando "
                                + "RESET_ADMIN_PASSWORD=true"
                );
            }
            if (passwordEncoder.matches(adminPassword, user.getPassword())) {
                logger.info(
                        "La contraseña de admin@uc.edu.co ya está restablecida"
                );
                return user;
            }

            user.setPassword(passwordEncoder.encode(adminPassword));
            user.setAuthProvider(AuthProvider.LOCAL_PASSWORD);
            User savedUser = userRepository.save(user);
            logger.info("Se restableció la contraseña de admin@uc.edu.co");
            return savedUser;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            logger.warn(
                    "No se creó admin@uc.edu.co: configura "
                            + "SEED_ADMIN_PASSWORD para habilitar el usuario semilla"
            );
            return null;
        }

        User user = new User();
        user.setName("Administrador");
        user.setEmail("admin@uc.edu.co");
        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setAuthProvider(AuthProvider.LOCAL_PASSWORD);
        return userRepository.save(user);
    }

    private Permission findOrCreatePermission(PermissionSeed seed) {
        return permissionRepository.findByUrlAndMethodAndEntity(
                        seed.url(),
                        seed.method(),
                        seed.entity()
                )
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setUrl(seed.url());
                    permission.setMethod(seed.method());
                    permission.setEntity(seed.entity());
                    return permissionRepository.save(permission);
                });
    }

    private void assignRole(User user, Role role) {
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }
    }

    private void assignPermission(Role role, Permission permission) {
        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(
                role.getId(),
                permission.getId())) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            rolePermissionRepository.save(rolePermission);
        }
    }

    private record PermissionSeed(String method, String url, String entity) {
    }
}