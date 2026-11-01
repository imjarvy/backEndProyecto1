package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.TwoFactorChallengeResponse;
import com.uc.ms_security.dto.auth.TwoFactorVerificationDTO;
import com.uc.ms_security.dto.auth.NotificationTwoFactorResponse;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final SessionService sessionService;
    private final NotificationService notificationService;

    @Transactional
    public Challenge createChallenge(User user) {
        NotificationTwoFactorResponse notificationChallenge =
                notificationService.requestTwoFactorCode(user.getEmail());
        Instant expiresAt = Instant.now().plusSeconds(
                notificationChallenge.expiresIn()
        );

        sessionService.createPendingAuthenticationSession(
                user,
                notificationChallenge.challengeId(),
                null,
                LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
        );

        return new Challenge(
                new TwoFactorChallengeResponse(
                        notificationChallenge.challengeId(),
                        expiresAt
                )
        );
    }

    @Transactional
    public Session verifyChallenge(TwoFactorVerificationDTO dto) {
        Session session = sessionService
                .findByTwoFactorChallengeId(dto.getChallengeId())
                .filter(candidate -> !candidate.isActive())
                .orElseThrow(this::invalidCode);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (session.getTwoFactorExpiresAt() == null
                || !session.getTwoFactorExpiresAt().isAfter(now)
                || !notificationService.verifyTwoFactor(
                        session.getUser().getEmail(),
                        dto.getChallengeId(),
                        dto.getCode())) {
            throw invalidCode();
        }

        sessionService.consumeTwoFactorCode(session);
        return session;
    }

    private ApplicationException invalidCode() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "El código de verificación es inválido o expiró"
        );
    }

    public record Challenge(
            TwoFactorChallengeResponse response) {
    }
}
