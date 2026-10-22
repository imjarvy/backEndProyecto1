package com.uc.ms_security.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BasePermissionDTO {

    @NotBlank(
            message = "La url es obligatoria"
    )
    @Size(
            max = 255,
            message = "La url debe tener máximo 255 caracteres"
    )
    private String url;

    @NotBlank(
            message = "El método es obligatorio"
    )
    @Pattern(
            regexp = "^(GET|POST|PUT|PATCH|DELETE)$",
            message = "El método debe ser GET, POST, PUT, PATCH o DELETE"
    )
    private String method;

    @NotBlank(
            message = "El modelo es obligatorio"
    )
    @Size(
            max = 100,
            message = "El modelo debe tener máximo 100 caracteres"
    )
    private String model;
}
