package com.uc.ms_security.repository;

import com.uc.ms_security.entity.PasswordRecoveryToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordRecoveryTokenRepository
        extends JpaRepository<PasswordRecoveryToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select recoveryToken
            from PasswordRecoveryToken recoveryToken
            where recoveryToken.tokenHash = :tokenHash
            """)
    Optional<PasswordRecoveryToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("""
            delete from PasswordRecoveryToken recoveryToken
            where recoveryToken.user.id = :userId
            """)
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            delete from PasswordRecoveryToken recoveryToken
            where recoveryToken.tokenHash = :tokenHash
            """)
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);
}
