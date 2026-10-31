package com.uc.ms_security.dto.auth;

public record FirebaseIdentityDTO(
        String uid,
        String email,
        String name,
        String provider) {
}
