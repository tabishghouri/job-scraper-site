package com.jobtracker.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Maps personal scraper API keys to the uid that owns them. Only a SHA-256
 * hash of each key is ever stored — same model as a GitHub personal access
 * token — so a Firestore export never contains a usable secret.
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String COLLECTION = "apiKeys";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Firestore firestore;

    /** Generates a fresh random raw key. Not stored anywhere by this call alone. */
    public String generateRawKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e); // SHA-256 is always available
        }
    }

    public void storeMapping(String rawKeyHash, String uid) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION).document(rawKeyHash)
                .set(Map.of("uid", uid, "createdAt", System.currentTimeMillis()))
                .get();
    }

    public void deleteMapping(String rawKeyHash) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION).document(rawKeyHash).delete().get();
    }

    /** Resolves a raw key from a request header to the uid that owns it, or null. */
    public String resolveUid(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return null;
        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION).document(hash(rawKey)).get().get();
            return doc.exists() ? doc.getString("uid") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
