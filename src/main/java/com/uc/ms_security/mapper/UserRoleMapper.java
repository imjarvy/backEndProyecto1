package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.role.RoleResponseDTO;
import com.uc.ms_security.dto.user.UserResponseDTO;
import com.uc.ms_security.dto.userrole.RoleUserResponseDTO;
import com.uc.ms_security.dto.userrole.UserRoleResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserRoleMapper {

    public UserRoleResponseDTO toResponseDTO(UserRole userRole) {
        return new UserRoleResponseDTO(
                userRole.getId(),
                userRole.getUser().getId(),
                new RoleResponseDTO(
                        userRole.getRole().getId(),
                        userRole.getRole().getName(),
                        userRole.getRole().getDescription()
                )
        );
    }

    public List<UserRoleResponseDTO> toResponseDTOList(List<UserRole> userRoles) {
        return userRoles.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public RoleUserResponseDTO toRoleUserResponseDTO(UserRole userRole) {
        User user = userRole.getUser();

        return new RoleUserResponseDTO(
                userRole.getId(),
                userRole.getRole().getId(),
                new UserResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                )
        );
    }

    public List<RoleUserResponseDTO> toRoleUserResponseDTOList(List<UserRole> userRoles) {
        return userRoles.stream()
                .map(this::toRoleUserResponseDTO)
                .toList();
    }
}
