package com.uc.ms_security.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequestDTO(
        @NotBlank(message = "El token es obligatorio")
        @Size(max = 128, message = "El token no tiene un formato válido")
        String token,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(
                min = 8,
                max = 72,
                message = "La contraseña debe tener entre 8 y 72 caracteres"
        )
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])\\S{8,72}$",
                message = "La contraseña debe contener al menos una mayúscula, "
                        + "una minúscula, un número y un carácter especial, "
                        + "sin espacios"
        )
        String newPassword) {
}
