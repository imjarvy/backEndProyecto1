package com.uc.ms_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthLoginRequestDTO {

    @NotBlank(message = "El token de Firebase es obligatorio")
    private String idToken;
}
