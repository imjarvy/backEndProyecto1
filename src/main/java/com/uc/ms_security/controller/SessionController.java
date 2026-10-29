package com.uc.ms_security.controller;

import com.uc.ms_security.dto.session.SessionRequestDTO;
import com.uc.ms_security.dto.session.SessionResponseDTO;
import com.uc.ms_security.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponseDTO create(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SessionRequestDTO dto) {

        return sessionService.create(userId, authenticatedUserId(jwt), dto);
    }

    @GetMapping
    public List<SessionResponseDTO> findAll(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {

        return sessionService.findAllByUserId(
                userId,
                authenticatedUserId(jwt)
        );
    }

    @GetMapping("/{sessionId}")
    public SessionResponseDTO findById(
            @PathVariable Long userId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        return sessionService.findById(
                userId,
                authenticatedUserId(jwt),
                sessionId
        );
    }

    @PutMapping("/{sessionId}")
    public SessionResponseDTO update(
            @PathVariable Long userId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SessionRequestDTO dto) {

        return sessionService.update(
                userId,
                authenticatedUserId(jwt),
                sessionId,
                dto
        );
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        sessionService.delete(
                userId,
                authenticatedUserId(jwt),
                sessionId
        );
    }

    private Long authenticatedUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
