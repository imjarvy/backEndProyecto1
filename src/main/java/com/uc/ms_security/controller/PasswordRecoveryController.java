package com.uc.ms_security.controller;

import com.uc.ms_security.dto.auth.PasswordRecoveryRequestDTO;
import com.uc.ms_security.dto.auth.PasswordRecoveryResponseDTO;
import com.uc.ms_security.dto.auth.PasswordResetRequestDTO;
import com.uc.ms_security.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-recovery")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/request")
    public ResponseEntity<PasswordRecoveryResponseDTO> request(
            @Valid @RequestBody PasswordRecoveryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PasswordRecoveryResponseDTO(
                        passwordRecoveryService.requestRecovery(request)
                ));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(
            @Valid @RequestBody PasswordResetRequestDTO request) {
        passwordRecoveryService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
