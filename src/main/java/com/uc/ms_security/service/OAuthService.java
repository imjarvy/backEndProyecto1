package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.OAuthLoginRequestDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.uc.ms_security.dto.auth.VerifiedIdentity;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final TokenVerifier tokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final RegistrationService registrationService;
    
    public AuthResponseDTO login(OAuthLoginRequestDTO dto) {
        VerifiedIdentity identity = tokenVerifier.verify(dto.getIdToken());

        User user = userRepository.findByEmail(identity.email())
                .orElseGet(() -> registrationService.register(identity));

        AuthResponseDTO token = jwtService.generateToken(user);
        sessionService.registerAuthenticationSession(
                user,
                token.tokenId(),
                token.expiresAt()
        );
        return token;
    }
}
