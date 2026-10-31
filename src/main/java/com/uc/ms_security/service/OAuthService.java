package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.OAuthLoginRequestDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.uc.ms_security.dto.auth.VerifiedIdentity;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final TokenVerifier tokenVerifier;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;        

    private static final String DEFAULT_ROLE = "CONSULTOR";
    private static final int MAX_NAME_LENGTH = 100;
    
    @Transactional
    public AuthResponseDTO login(OAuthLoginRequestDTO dto) {
        VerifiedIdentity identity = tokenVerifier.verify(dto.getIdToken());

        User user = userRepository.findByEmail(identity.email())
                .orElseGet(() -> createOAuthUser(identity));

        AuthResponseDTO token = jwtService.generateToken(user);
        sessionService.registerAuthenticationSession(
                user,
                token.tokenId(),
                token.expiresAt()
        );
        return token;
    }

    private User createOAuthUser(VerifiedIdentity identity) {
        Role role = roleRepository.findByNameIgnoreCase(DEFAULT_ROLE)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.INVALID_OPERATION,
                        "El rol CONSULTOR no está configurado"
                ));

        User user = new User();
        user.setName(resolveName(identity));
        user.setEmail(identity.email());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        User savedUser = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(savedUser);
        userRole.setRole(role);
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
