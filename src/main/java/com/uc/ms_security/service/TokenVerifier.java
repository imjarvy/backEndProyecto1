package com.uc.ms_security.service;

import com.uc.ms_security.dto.auth.VerifiedIdentity;

public interface TokenVerifier {

    VerifiedIdentity verify(String token);
}
