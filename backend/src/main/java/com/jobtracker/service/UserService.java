package com.jobtracker.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String COLLECTION = "users";

    private final Firestore firestore;
    private final ApiKeyService apiKeyService;

    /** Returns { uid, hasApiKey, apiKeyLast4 }, provisioning the profile + first key on first call. */
    public Map<String, Object> getOrCreateProfile(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(uid).get().get();
        if (!doc.exists()) {
            String rawKey = regenerateKey(uid);
            return profileResponse(uid, rawKey.substring(rawKey.length() - 4));
        }
        return profileResponse(uid, doc.getString("apiKeyLast4"));
    }

    /** Rotates uid's personal API key. Returns the new raw key — shown once, never stored raw. */
    public String regenerateKey(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot existing = firestore.collection(COLLECTION).document(uid).get().get();
        if (existing.exists() && existing.getString("apiKeyHash") != null) {
            apiKeyService.deleteMapping(existing.getString("apiKeyHash"));
        }

        String rawKey = apiKeyService.generateRawKey();
        String hash = apiKeyService.hash(rawKey);
        apiKeyService.storeMapping(hash, uid);

        Map<String, Object> fields = new HashMap<>();
        fields.put("apiKeyHash", hash);
        fields.put("apiKeyLast4", rawKey.substring(rawKey.length() - 4));
        fields.put("apiKeyCreatedAt", System.currentTimeMillis());
        firestore.collection(COLLECTION).document(uid).set(fields, SetOptions.merge()).get();

        return rawKey;
    }

    private Map<String, Object> profileResponse(String uid, String apiKeyLast4) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("uid", uid);
        resp.put("hasApiKey", apiKeyLast4 != null);
        resp.put("apiKeyLast4", apiKeyLast4);
        return resp;
    }
}
