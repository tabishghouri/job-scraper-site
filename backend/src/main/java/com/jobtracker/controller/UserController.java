package com.jobtracker.controller;

import com.jobtracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GET  /api/me                 -> { uid, hasApiKey, apiKeyLast4 } (lazy-provisions on first call)
 * POST /api/me/regenerate-key  -> { uid, apiKey } (raw key, shown once — copy it into scraper/.env)
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
}
