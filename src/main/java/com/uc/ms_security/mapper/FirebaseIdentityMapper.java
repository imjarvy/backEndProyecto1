package com.uc.ms_security.mapper;

import com.google.firebase.auth.FirebaseToken;
import com.uc.ms_security.dto.auth.FirebaseIdentityDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FirebaseIdentityMapper {

    public FirebaseIdentityDTO toIdentity(FirebaseToken token) {
        return new FirebaseIdentityDTO(
                token.getUid(),
                token.getEmail(),
                token.getName(),
                signInProvider(token)
        );
    }

    private String signInProvider(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get("firebase");

        if (firebaseClaim instanceof Map<?, ?> firebase) {
            Object provider = firebase.get("sign_in_provider");
            return provider instanceof String ? (String) provider : null;
        }

        return null;
    }
}
