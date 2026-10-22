package com.uc.ms_security.dto.role;

import com.uc.ms_security.dto.rolepermission.RolePermissionResponseDTO;
import lombok.Value;

import java.util.List;

@Value
public class RolePermissionsResponseDTO {

    Long id;

    String name;

    String description;

    List<RolePermissionResponseDTO> permissions;
}
