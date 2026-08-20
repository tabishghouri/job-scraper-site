package com.jobtracker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Initializes Firebase Admin SDK on startup and exposes Firestore + Storage beans.
 * The service account JSON gives the backend full admin access to both,
 * bypassing security rules (rules only apply to client SDKs).
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Value("${firebase.project.id}")
    private String projectId;

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FileInputStream serviceAccount = new FileInputStream(credentialsPath);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId(projectId)
                .setStorageBucket(storageBucket)
                .build();
        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("Firebase initialized for project: {}", projectId);
        return app;
    }

    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }

    @Bean
    public Bucket bucket(FirebaseApp app) {
        return StorageClient.getInstance(app).bucket(storageBucket);
    }
}
