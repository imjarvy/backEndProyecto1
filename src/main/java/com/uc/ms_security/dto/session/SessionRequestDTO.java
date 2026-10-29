package com.uc.ms_security.dto.session;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SessionRequestDTO {

    @NotBlank(message = "El identificador de sesión es obligatorio")
    @Size(max = 36, message = "El identificador de sesión es demasiado largo")
    private String tokenId;

    @NotNull(
            message = "La fecha de expiración es obligatoria"
    )
    @Future(
            message = "La fecha de expiración debe estar en el futuro"
    )
    private LocalDateTime expiration;

}
