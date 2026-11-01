package com.uc.ms_security.dto.auth;

import java.time.Instant;

public record TwoFactorChallengeResponse(
        String challengeId,
        Instant expiresAt) {
}
