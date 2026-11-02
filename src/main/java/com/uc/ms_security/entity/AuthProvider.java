package com.uc.ms_security.entity;

public enum AuthProvider {
    LOCAL_PASSWORD,
    GOOGLE,
    MICROSOFT,
    GITHUB,
    UNKNOWN;

    public static AuthProvider fromFirebaseProvider(String provider) {
        return switch (provider) {
            case "google.com" -> GOOGLE;
            case "microsoft.com" -> MICROSOFT;
            case "github.com" -> GITHUB;
            default -> UNKNOWN;
        };
    }
}
