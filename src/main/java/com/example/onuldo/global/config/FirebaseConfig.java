package com.example.onuldo.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
            if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                log.info("Firebase service account path is empty. Skip Firebase initialization.");
                return;
            }

            if (!FirebaseApp.getApps().isEmpty()) return;

            Resource resource = resourceLoader.getResource(serviceAccountPath);

            try (InputStream is = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(is))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Completed Firebase application initialize");
            }
        } catch (Exception e) {
            log.error("Firebase 초기화 실패", e);
            throw new IllegalStateException("Firebase 초기화 실패", e);
        }
    }
}
