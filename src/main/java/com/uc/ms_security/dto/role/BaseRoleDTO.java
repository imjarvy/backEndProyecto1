package com.uc.ms_security.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseRoleDTO {

    @NotBlank(
            message = "El nombre es obligatorio"
    )
    @Size(
            min = 2,
            max = 50,
            message = "El nombre debe tener entre 2 y 50 caracteres"
    )
    private String name;

    @Size(
            max = 255,
            message = "La descripción debe tener máximo 255 caracteres"
    )
    private String description;
}
