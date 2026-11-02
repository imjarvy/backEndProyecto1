package com.uc.ms_security.service;

import com.uc.ms_security.entity.AuthProvider;
import com.uc.ms_security.entity.PasswordRecoveryToken;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.repository.PasswordRecoveryTokenRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryTokenService {

    private final UserRepository userRepository;
    private final PasswordRecoveryTokenRepository tokenRepository;

    @Transactional
    public Optional<PasswordRecoveryToken> issueForLocalUser(
            String email,
            String tokenHash) {
        Optional<User> matchingUser = userRepository.findByEmailForUpdate(email);
        if (matchingUser.isEmpty()
                || matchingUser.get().getAuthProvider()
                != AuthProvider.LOCAL_PASSWORD) {
            return Optional.empty();
        }

        User user = matchingUser.get();
        tokenRepository.deleteAllByUserId(user.getId());

        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
        PasswordRecoveryToken recoveryToken = new PasswordRecoveryToken();
        recoveryToken.setUser(user);
        recoveryToken.setTokenHash(tokenHash);
        recoveryToken.setCreatedAt(createdAt);
        recoveryToken.setExpiresAt(createdAt.plusMinutes(30));

        return Optional.of(tokenRepository.save(recoveryToken));
    }

    @Transactional
    public void invalidateByHash(String tokenHash) {
        tokenRepository.deleteByTokenHash(tokenHash);
    }
}
