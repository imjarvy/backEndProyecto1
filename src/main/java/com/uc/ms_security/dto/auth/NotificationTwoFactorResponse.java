package com.uc.ms_security.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

public record NotificationTwoFactorResponse(
        @JsonAlias({"challenge_id", "challengeId"})
        String challengeId,
        @JsonAlias({"expires_in", "expiresIn"})
        long expiresIn) {
}
