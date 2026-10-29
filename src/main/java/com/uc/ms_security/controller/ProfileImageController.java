package com.uc.ms_security.controller;

import com.uc.ms_security.service.ProfileImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/profile-images")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageStorage profileImageStorage;

    @GetMapping("/{imageKey:.+}")
    public ResponseEntity<Resource> get(@PathVariable String imageKey)
            throws IOException {
        Resource image = profileImageStorage.load(imageKey);
        String contentType = Files.probeContentType(image.getFile().toPath());
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(image);
    }
}
