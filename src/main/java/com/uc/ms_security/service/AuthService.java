package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.LoginRequestDTO;
import com.uc.ms_security.dto.auth.TwoFactorChallengeResponse;
import com.uc.ms_security.dto.auth.TwoFactorVerificationDTO;
import com.uc.ms_security.dto.user.CreateUserDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final RegistrationService registrationService;
    private final TwoFactorService twoFactorService;

    public TwoFactorChallengeResponse login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(this::unauthorized);

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw unauthorized();
        }

        return twoFactorService.createChallenge(user).response();
    }

    public TwoFactorChallengeResponse register(CreateUserDTO dto) {
        User savedUser = registrationService.register(dto);
        return twoFactorService.createChallenge(savedUser).response();
    }

    public AuthResponseDTO verifyTwoFactor(TwoFactorVerificationDTO dto) {
        Session session = twoFactorService.verifyChallenge(dto);
        AuthResponseDTO token = jwtService.generateToken(session.getUser());

        sessionService.activateAuthenticationSession(
                session,
                token.tokenId(),
                token.expiresAt()
        );
        return token;
    }

    private ApplicationException unauthorized() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Credenciales incorrectas"
        );
    }
}
