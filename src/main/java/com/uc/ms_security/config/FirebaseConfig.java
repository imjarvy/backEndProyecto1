package com.uc.ms_security.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp(
            @Value("${firebase.project-id:}") String projectId,
            @Value("${firebase.credentials-path:}") String credentialsPath)
            throws IOException {

        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException(
                    "firebase.project-id es obligatorio cuando firebase.enabled=true"
            );
        }

        FirebaseOptions.Builder options = FirebaseOptions.builder()
                .setProjectId(projectId);

        if (StringUtils.hasText(credentialsPath)) {
            try (InputStream credentials = new FileInputStream(credentialsPath)) {
                options.setCredentials(
                        GoogleCredentials.fromStream(credentials)
                );
            }
        } else {
            options.setCredentials(GoogleCredentials.getApplicationDefault());
        }

        return FirebaseApp.initializeApp(options.build());
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
