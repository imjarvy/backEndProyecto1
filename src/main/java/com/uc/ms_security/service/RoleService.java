package com.uc.ms_security.service;

import com.uc.ms_security.dto.role.CreateRoleDTO;
import com.uc.ms_security.dto.role.RoleRequestDTO;
import com.uc.ms_security.dto.role.RoleResponseDTO;
import com.uc.ms_security.dto.role.RolePermissionsResponseDTO;
import com.uc.ms_security.dto.role.UpdateRoleDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.RoleMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;

    public RoleResponseDTO create(CreateRoleDTO dto) {
        if (roleRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un rol con ese nombre"
            );
        }
        Role role = roleMapper.toEntity(dto);
        Role savedRole = roleRepository.save(role);
        return roleMapper.toResponseDTO(savedRole);
    }

    public RoleResponseDTO create(RoleRequestDTO dto) {
        if (roleRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un rol con ese nombre"
            );
        }

        Role role = roleMapper.toEntity(dto);
        Role savedRole = roleRepository.save(role);

        return new RoleResponseDTO(
                savedRole.getId(),
                savedRole.getName(),
                savedRole.getDescription()
        );
    }

    public List<RoleResponseDTO> findAll() {
        List<Role> roles = roleRepository.findAll();
        return roleMapper.toResponseDTOList(roles);
    }
    private Role findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Rol no encontrado con id: " + id
                ));
    }

    public RoleResponseDTO findById(Long id) {
        Role role = findRole(id);
        return roleMapper.toResponseDTO(role);
    }

    public RolePermissionsResponseDTO findByIdAndPermissions(Long id) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Rol no encontrado con id: " + id
                ));
        return roleMapper.toPermissionsResponseDTO(role);
    }

    public RoleResponseDTO update(Long id, UpdateRoleDTO dto) {
        Role role = findRole(id);
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El nombre pertenece a otro rol"
            );
        }
        roleMapper.updateEntity(dto, role);
        Role updatedRole = roleRepository.save(role);
        return roleMapper.toResponseDTO(updatedRole);
    }

    public RoleResponseDTO update(Long id, RoleRequestDTO dto) {
        Role role = findRole(id);

        if (roleRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El nombre pertenece a otro rol"
            );
        }

        roleMapper.updateEntity(dto, role);
        Role updatedRole = roleRepository.save(role);

        return new RoleResponseDTO(
                updatedRole.getId(),
                updatedRole.getName(),
                updatedRole.getDescription()
        );
    }

    public void delete(Long id) {
        Role role = findRole(id);

        if (userRoleRepository.existsByRoleId(id)) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No se puede eliminar un rol que está asignado"
            );
        }

        roleRepository.delete(role);
    }
}
