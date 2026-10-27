package com.uc.ms_security.service;

import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalProfileImageStorage implements ProfileImageStorage {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Value("${profile-images.storage-path:uploads/profile-images}")
    private String storagePath;

    @Value("${profile-images.public-url:/api/profile-images}")
    private String publicUrl;

    @Override
    public StoredProfileImage store(MultipartFile image) {
        validate(image);

        String key = UUID.randomUUID() + EXTENSIONS.get(image.getContentType());
        Path directory = Paths.get(storagePath).toAbsolutePath().normalize();
        Path destination = directory.resolve(key).normalize();

        if (!destination.startsWith(directory)) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La ruta de la imagen no es válida"
            );
        }

        try {
            Files.createDirectories(directory);
            image.transferTo(destination);
            return new StoredProfileImage(publicUrl + "/" + key, key);
        } catch (IOException exception) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No fue posible almacenar la imagen"
            );
        }
    }

    @Override
    public void delete(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return;
        }

        Path directory = Paths.get(storagePath).toAbsolutePath().normalize();
        Path file = directory.resolve(imageKey).normalize();
        if (!file.startsWith(directory)) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La ruta de la imagen no es válida"
            );
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No fue posible eliminar la imagen"
            );
        }
    }

    @Override
    public Resource load(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            throw new ApplicationException(
                    ErrorCase.NOT_FOUND,
                    "Imagen no encontrada"
            );
        }

        Path directory = Paths.get(storagePath).toAbsolutePath().normalize();
        Path file = directory.resolve(imageKey).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new ApplicationException(
                    ErrorCase.NOT_FOUND,
                    "Imagen no encontrada"
            );
        }

        return new FileSystemResource(file);
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La imagen no puede estar vacía"
            );
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "La imagen no puede superar los 5 MB"
            );
        }
        if (!EXTENSIONS.containsKey(image.getContentType())) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "Solo se permiten imágenes JPG, PNG o WEBP"
            );
        }
    }
}
