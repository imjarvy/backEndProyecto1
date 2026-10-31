package com.uc.ms_security.service;

import com.uc.ms_security.dto.session.SessionRequestDTO;
import com.uc.ms_security.dto.session.SessionResponseDTO;
import com.uc.ms_security.entity.Session;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.SessionMapper;
import com.uc.ms_security.repository.SessionRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionMapper sessionMapper;

    public SessionResponseDTO create(
            Long userId,
            Long authenticatedUserId,
            SessionRequestDTO dto) {

        requireOwner(userId, authenticatedUserId);
        User user = findUser(userId);

        if (sessionRepository.existsByTokenId(dto.getTokenId())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El identificador de sesión ya está registrado"
            );
        }

        Session session = sessionMapper.toEntity(dto);
        session.setUser(user);

        Session savedSession = sessionRepository.save(session);

        return sessionMapper.toResponseDTO(savedSession);
    }

    public List<SessionResponseDTO> findAllByUserId(
            Long userId,
            Long authenticatedUserId) {
        requireOwner(userId, authenticatedUserId);
        findUser(userId);

        List<Session> sessions =
                sessionRepository.findAllByUserId(userId);

        return sessionMapper.toResponseDTOList(sessions);
    }

    public SessionResponseDTO findById(
            Long userId,
            Long authenticatedUserId,
            Long sessionId) {
        requireOwner(userId, authenticatedUserId);

        return sessionMapper.toResponseDTO(
                findSession(userId, sessionId)
        );
    }

    public SessionResponseDTO update(
            Long userId,
            Long authenticatedUserId,
            Long sessionId,
            SessionRequestDTO dto) {
        requireOwner(userId, authenticatedUserId);

        Session session = findSession(userId, sessionId);

        if (sessionRepository.existsByTokenIdAndIdNot(
                dto.getTokenId(),
                sessionId)) {

            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El identificador de sesión ya está registrado"
            );
        }

        sessionMapper.updateEntity(dto, session);

        Session updatedSession = sessionRepository.save(session);

        return sessionMapper.toResponseDTO(updatedSession);
    }

    public void delete(
            Long userId,
            Long authenticatedUserId,
            Long sessionId) {
        requireOwner(userId, authenticatedUserId);

        sessionRepository.delete(
                findSession(userId, sessionId)
        );
    }

    public void registerAuthenticationSession(
            User user,
            String tokenId,
            java.time.Instant expiresAt) {
        Session session = new Session();
        session.setTokenId(tokenId);
        session.setExpiration(
                LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
        );
        session.setUser(user);
        sessionRepository.save(session);
    }

    @Transactional
    public void revokeAuthenticationSession(String tokenId) {
        if (tokenId != null && !tokenId.isBlank()) {
            sessionRepository.deleteByTokenId(tokenId);
        }
    }

    private void requireOwner(Long userId, Long authenticatedUserId) {
        if (!Objects.equals(userId, authenticatedUserId)) {
            throw new AccessDeniedException(
                    "No está autorizado para acceder a las sesiones de este usuario"
            );
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                    () -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + userId
                        )
                );
    }

    private Session findSession(
            Long userId,
            Long sessionId) {

        return sessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(
                    () -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Sesión no encontrada para este usuario"
                        )
                );
    }
}
