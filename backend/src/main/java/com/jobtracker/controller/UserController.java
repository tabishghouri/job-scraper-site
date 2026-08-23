package com.jobtracker.controller;

import com.jobtracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GET   /api/me                 -> { uid, hasApiKey, apiKeyLast4, searchQueries, locations, jobLevel } (lazy-provisions on first call)
 * POST  /api/me/regenerate-key  -> { uid, apiKey } (raw key, shown once — copy it into scraper/.env)
 * PATCH /api/me/search-config   -> { searchQueries, locations, jobLevel } — custom scraper search preferences
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestAttribute("uid") String uid) {
        try {
            return ResponseEntity.ok(userService.getOrCreateProfile(uid));
        } catch (Exception e) {
            log.error("getProfile failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load profile"));
        }
    }

    @PostMapping("/regenerate-key")
    public ResponseEntity<?> regenerateKey(@RequestAttribute("uid") String uid) {
        try {
            String rawKey = userService.regenerateKey(uid);
            return ResponseEntity.ok(Map.of("uid", uid, "apiKey", rawKey));
        } catch (Exception e) {
            log.error("regenerateKey failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to regenerate key"));
        }
    }

    @SuppressWarnings("unchecked")
    @PatchMapping("/search-config")
    public ResponseEntity<?> updateSearchConfig(@RequestAttribute("uid") String uid, @RequestBody Map<String, Object> body) {
        try {
            List<String> queries = (List<String>) body.getOrDefault("searchQueries", List.of());
            List<String> locations = (List<String>) body.getOrDefault("locations", List.of());
            String jobLevel = (String) body.getOrDefault("jobLevel", "internship");
            userService.updateSearchConfig(uid, queries, locations, jobLevel);
            return ResponseEntity.ok(userService.getOrCreateProfile(uid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("updateSearchConfig failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save search config"));
        }
    }
}
