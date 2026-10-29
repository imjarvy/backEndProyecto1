package com.uc.ms_security.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorage {

    StoredProfileImage store(MultipartFile image);

    void delete(String imageKey);

    Resource load(String imageKey);

    record StoredProfileImage(String url, String key) {
    }
}