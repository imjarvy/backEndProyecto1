package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.VerifiedIdentity;
import com.uc.ms_security.dto.user.CreateUserDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String CONSULTANT_ROLE = "CONSULTOR";
    private static final int MAX_NAME_LENGTH = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(CreateUserDTO dto) {
        return createUser(
                dto.getName(),
                dto.getEmail(),
                dto.getPassword()
        );
    }

    @Transactional
    public User register(VerifiedIdentity identity) {
        return createUser(
                resolveName(identity),
                identity.email(),
                UUID.randomUUID().toString()
        );
    }

    private User createUser(String name, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un usuario con este email"
            );
        }

        Role consultantRole = roleRepository
                .findByNameIgnoreCase(CONSULTANT_ROLE)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.INVALID_OPERATION,
                        "El rol CONSULTOR no está configurado"
                ));

        CreateUserDTO dto = new CreateUserDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setPassword(rawPassword);

        User user = userMapper.toEntity(
                dto,
                passwordEncoder.encode(rawPassword)
        );
        User savedUser = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(savedUser);
        userRole.setRole(consultantRole);
        userRoleRepository.save(userRole);

        return savedUser;
    }

    private String resolveName(VerifiedIdentity identity) {
        String name = StringUtils.hasText(identity.name())
                ? identity.name().trim()
                : identity.email().substring(0, identity.email().indexOf('@'));

        return name.length() <= MAX_NAME_LENGTH
                ? name
                : name.substring(0, MAX_NAME_LENGTH);
    }
}
