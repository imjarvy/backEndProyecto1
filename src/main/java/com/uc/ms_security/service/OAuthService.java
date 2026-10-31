package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.AuthResponseDTO;
import com.uc.ms_security.dto.auth.FirebaseIdentityDTO;
import com.uc.ms_security.dto.auth.OAuthLoginRequestDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final FirebaseTokenService firebaseTokenService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponseDTO login(OAuthLoginRequestDTO dto) {
        FirebaseIdentityDTO identity =
                firebaseTokenService.verify(dto.getIdToken());

        User user = userRepository.findByEmail(identity.email())
                .orElseThrow(this::unauthorized);

        return jwtService.generateToken(user);
    }

    private ApplicationException unauthorized() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Credenciales incorrectas"
        );
    }
}
