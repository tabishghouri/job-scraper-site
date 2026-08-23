package com.jobtracker.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String COLLECTION = "users";
    private static final int MAX_QUERIES = 30;
    private static final int MAX_LOCATIONS = 10;
    private static final List<String> VALID_JOB_LEVELS = List.of("internship", "entry_level");

    private final Firestore firestore;
    private final ApiKeyService apiKeyService;

    /** Returns { uid, hasApiKey, apiKeyLast4, searchQueries, locations, jobLevel }, provisioning the profile + first key on first call. */
    public Map<String, Object> getOrCreateProfile(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(uid).get().get();
        if (!doc.exists()) {
            String rawKey = regenerateKey(uid);
            doc = firestore.collection(COLLECTION).document(uid).get().get();
            return profileResponse(uid, rawKey.substring(rawKey.length() - 4), doc);
        }
        return profileResponse(uid, doc.getString("apiKeyLast4"), doc);
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

    /** Saves uid's custom scraper search config. Empty lists mean "use the scraper's built-in defaults". */
    public void updateSearchConfig(String uid, List<String> searchQueries, List<String> locations, String jobLevel)
            throws ExecutionException, InterruptedException {
        if (!VALID_JOB_LEVELS.contains(jobLevel)) {
            throw new IllegalArgumentException("Invalid jobLevel: " + jobLevel);
        }

        Map<String, Object> fields = new HashMap<>();
        fields.put("searchQueries", cleanList(searchQueries, MAX_QUERIES));
        fields.put("locations", cleanList(locations, MAX_LOCATIONS));
        fields.put("jobLevel", jobLevel);
        firestore.collection(COLLECTION).document(uid).set(fields, SetOptions.merge()).get();
    }

    /** Returns { searchQueries, locations, jobLevel } for the scraper to fetch at startup. */
    public Map<String, Object> getSearchConfig(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(uid).get().get();
        Map<String, Object> resp = new HashMap<>();
        resp.put("searchQueries", doc.exists() ? doc.get("searchQueries") : List.of());
        resp.put("locations", doc.exists() ? doc.get("locations") : List.of());
        resp.put("jobLevel", doc.exists() && doc.getString("jobLevel") != null ? doc.getString("jobLevel") : "internship");
        return resp;
    }

    private List<String> cleanList(List<String> values, int maxSize) {
        if (values == null) return List.of();
        return values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(maxSize)
                .collect(Collectors.toList());
    }

    private Map<String, Object> profileResponse(String uid, String apiKeyLast4, DocumentSnapshot doc) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("uid", uid);
        resp.put("hasApiKey", apiKeyLast4 != null);
        resp.put("apiKeyLast4", apiKeyLast4);
        resp.put("searchQueries", doc.exists() && doc.get("searchQueries") != null ? doc.get("searchQueries") : List.of());
        resp.put("locations", doc.exists() && doc.get("locations") != null ? doc.get("locations") : List.of());
        resp.put("jobLevel", doc.exists() && doc.getString("jobLevel") != null ? doc.getString("jobLevel") : "internship");
        return resp;
    }
}
