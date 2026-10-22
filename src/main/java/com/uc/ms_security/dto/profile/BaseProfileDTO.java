package com.uc.ms_security.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public abstract class BaseProfileDTO {

    @NotBlank(
            message = "El teléfono es obligatorio"
    )
    @Pattern(
            regexp = "^\\+?[0-9]{7,15}$",
            message = "El teléfono debe tener entre 7 y 15 dígitos"
    )
    private String phone;

    @NotNull(
            message = "La fecha de nacimiento es obligatoria"
    )
    @Past(
            message = "La fecha de nacimiento debe ser una fecha pasada"
    )
    private LocalDate birthDate;
}
