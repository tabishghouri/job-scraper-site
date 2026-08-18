package com.jobtracker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Initializes Firebase Admin SDK on startup and exposes a Firestore bean.
 * The service account JSON gives the backend full admin access to Firestore,
 * bypassing security rules (rules only apply to client SDKs).
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Value("${firebase.project.id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FileInputStream serviceAccount = new FileInputStream(credentialsPath);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized for project: {}", projectId);
        }
        return FirestoreClient.getFirestore();
    }
}
