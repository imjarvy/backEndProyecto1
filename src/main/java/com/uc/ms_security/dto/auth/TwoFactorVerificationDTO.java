package com.uc.ms_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorVerificationDTO {

    @NotBlank(message = "El desafío es obligatorio")
    private String challengeId;

    @NotBlank(message = "El código es obligatorio")
    @Pattern(
            regexp = "\\d{6}",
            message = "El código debe tener exactamente 6 dígitos"
    )
    private String code;
}
