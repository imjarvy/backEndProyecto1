package com.uc.ms_security.dto.auth;

public interface VerifiedIdentity {

    String email();
    String name();
    String provider();
}
