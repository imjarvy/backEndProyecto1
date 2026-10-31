package com.uc.ms_security.service;

import com.uc.ms_security.dto.profile.CreateProfileDTO;
import com.uc.ms_security.dto.profile.UpdateProfileDTO;
import com.uc.ms_security.dto.profile.ProfileResponseDTO;
import com.uc.ms_security.entity.Profile;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.ProfileMapper;
import com.uc.ms_security.repository.ProfileRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final ProfileImageStorage profileImageStorage;

    @Transactional
    public ProfileResponseDTO create(Long userId, CreateProfileDTO dto) {
        return create(userId, dto, null);
    }

    @Transactional
    public ProfileResponseDTO create(
            Long userId,
            CreateProfileDTO dto,
            MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Usuario no encontrado con id: " + userId
                ));

        if (profileRepository.existsByUserId(userId)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "El usuario ya tiene un perfil"
            );
        }

        Profile profile = profileMapper.toEntity(dto);
        profile.setUser(user);

        ProfileImageStorage.StoredProfileImage storedImage = null;
        try {
            if (image != null && !image.isEmpty()) {
                storedImage = profileImageStorage.store(image);
                profile.setProfileImageUrl(storedImage.url());
                profile.setProfileImageKey(storedImage.key());
            }

            Profile savedProfile = profileRepository.save(profile);
            return profileMapper.toResponseDTO(savedProfile);
        } catch (RuntimeException exception) {
            if (storedImage != null) {
                profileImageStorage.delete(storedImage.key());
            }
            throw exception;
        }
    }

    @Transactional
    public ProfileResponseDTO update(Long userId, UpdateProfileDTO dto) {
        return update(userId, dto, null);
    }

    @Transactional
    public ProfileResponseDTO update(
            Long userId,
            UpdateProfileDTO dto,
            MultipartFile image) {
        Profile profile = findProfile(userId);

        profileMapper.updateEntity(dto, profile);
        String oldImageKey = profile.getProfileImageKey();
        ProfileImageStorage.StoredProfileImage storedImage = null;
        try {
            if (image != null && !image.isEmpty()) {
                storedImage = profileImageStorage.store(image);
                profile.setProfileImageUrl(storedImage.url());
                profile.setProfileImageKey(storedImage.key());
            }

            Profile updatedProfile = profileRepository.save(profile);
            if (storedImage != null && oldImageKey != null) {
                profileImageStorage.delete(oldImageKey);
            }
            return profileMapper.toResponseDTO(updatedProfile);
        } catch (RuntimeException exception) {
            if (storedImage != null) {
                profileImageStorage.delete(storedImage.key());
            }
            throw exception;
        }
    }

    public ProfileResponseDTO findByUserId(Long userId) {
        return profileMapper.toResponseDTO(findProfile(userId));
    }

    @Transactional
    public void delete(Long userId) {
        Profile profile = findProfile(userId);
        profileRepository.delete(profile);
        profileImageStorage.delete(profile.getProfileImageKey());
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCase.NOT_FOUND,
                        "Perfil no encontrado para el usuario con id: " + userId
                ));
    }
}
