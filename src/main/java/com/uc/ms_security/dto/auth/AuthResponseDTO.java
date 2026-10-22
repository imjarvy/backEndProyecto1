package com.uc.ms_security.dto.auth;

import java.time.Instant;

public record AuthResponseDTO(
        String accessToken,
        String tokenType,
        Instant expiresAt) {
}
