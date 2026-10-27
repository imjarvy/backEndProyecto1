package com.uc.ms_security.service;

import com.uc.ms_security.dto.permission.CreatePermissionDTO;
import com.uc.ms_security.dto.permission.PermissionResponseDTO;
import com.uc.ms_security.dto.permission.UpdatePermissionDTO;
import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.PermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final PermissionMapper permissionMapper;

    public PermissionResponseDTO create(CreatePermissionDTO dto) {
        if (permissionRepository.existsByUrlAndMethodAndEntity(
                dto.getUrl(),
                dto.getMethod(),
                dto.getEntity()
        )) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un permiso para esta url, método y entidad"
            );
        }
        Permission permission = permissionMapper.toEntity(dto);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(savedPermission);
    }
    public List<PermissionResponseDTO> findAll() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissionMapper.toResponseDTOList(permissions);
    }
    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Permiso no encontrado con id: " + id
                ));
    }

    public PermissionResponseDTO findById(Long id) {
        Permission permission = findPermission(id);
        return permissionMapper.toResponseDTO(permission);
    }

    public PermissionResponseDTO update(Long id, UpdatePermissionDTO dto) {
        Permission permission = findPermission(id);
        if (permissionRepository.existsByUrlAndMethodAndEntityAndIdNot(
                dto.getUrl(),
                dto.getMethod(),
                dto.getEntity(),
                id
        )) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un permiso con esa url, método y entidad"
            );
        }
        permissionMapper.updateEntity(dto, permission);
        Permission updatedPermission = permissionRepository.save(permission);
        return permissionMapper.toResponseDTO(updatedPermission);
    }
    public void delete(Long id) {
        Permission permission = findPermission(id);
        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No se puede eliminar un permiso que está asignado"
            );
        }
        permissionRepository.delete(permission);
    }

    public boolean hasPermission(
            Long userId,
            String httpMethod,
            String requestUrl) {
        String normalizedUrl = normalizeUrl(requestUrl);

        return !permissionRepository.findByUserIdAndMethodAndUrl(
                userId,
                httpMethod.toUpperCase(Locale.ROOT),
                normalizedUrl
        ).isEmpty();
    }

    private String normalizeUrl(String requestUrl) {
        return requestUrl.replaceAll(
                "(?i)(?<=/)(?:\\d+|[a-f0-9]{24}|[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})(?:\\.[a-z0-9]+)?(?=/|$)",
                "{id}"
        );
    }
}
