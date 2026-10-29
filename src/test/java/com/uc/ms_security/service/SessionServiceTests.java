package com.uc.ms_security.service;

import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.mapper.SessionMapper;
import com.uc.ms_security.repository.SessionRepository;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SessionServiceTests {

    private final SessionRepository sessionRepository =
            mock(SessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SessionMapper sessionMapper = mock(SessionMapper.class);

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                sessionRepository,
                userRepository,
                sessionMapper
        );
    }

    @Test
    void registersAuthenticationSessionWithTokenIdAndUtcExpiration() {
        User user = new User();
        user.setId(8L);
        Instant expiresAt = Instant.parse("2026-10-29T12:30:00Z");

        sessionService.registerAuthenticationSession(
                user,
                "jti-123",
                expiresAt
        );

        var sessionCaptor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        Session session = sessionCaptor.getValue();
        assertEquals("jti-123", session.getTokenId());
        assertEquals(
                LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC),
                session.getExpiration()
        );
        assertEquals(user, session.getUser());
    }

    @Test
    void revokesAuthenticationSessionByTokenId() {
        sessionService.revokeAuthenticationSession("jti-123");

        verify(sessionRepository).deleteByTokenId("jti-123");
    }

    @Test
    void doesNotAttemptToRevokeBlankTokenId() {
        sessionService.revokeAuthenticationSession(" ");

        verifyNoInteractions(sessionRepository);
    }

    @Test
    void rejectsCrossUserSessionAccessBeforeReadingRepositories() {
        assertThrows(
                AccessDeniedException.class,
                () -> sessionService.findAllByUserId(22L, 11L)
        );

        verifyNoInteractions(
                sessionRepository,
                userRepository,
                sessionMapper
        );
    }

    @Test
    void rejectsCrossUserSessionDeletionBeforeReadingRepositories() {
        assertThrows(
                AccessDeniedException.class,
                () -> sessionService.delete(22L, 11L, 4L)
        );

        verifyNoInteractions(
                sessionRepository,
                userRepository,
                sessionMapper
        );
    }
}
