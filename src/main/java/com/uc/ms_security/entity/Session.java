package com.uc.ms_security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(name = "token", unique = true, length = 500, nullable = true)
    private String tokenId;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "code_2fa", length = 100)
    private String twoFactorCode;

    @Column(name = "two_fa_expires_at")
    private LocalDateTime twoFactorExpiresAt;

    @Column(name = "two_fa_challenge_id", unique = true, length = 255)
    private String twoFactorChallengeId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;
}
