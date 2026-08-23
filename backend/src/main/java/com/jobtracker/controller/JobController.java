package com.jobtracker.controller;

import com.jobtracker.model.Job;
import com.jobtracker.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API — exposes endpoints consumed by the React frontend and Python scraper.
 * Every job is scoped to the caller's uid, resolved by AuthInterceptor from either
 * a Firebase ID token (browser) or a personal API key (scraper) and passed in as
 * a request attribute — routes here never see or trust a uid from the client itself.
 *
 * GET    /api/jobs              → all jobs for the caller (?status=applied, ?search=shopify)
 * GET    /api/jobs/stats        → dashboard counts
 * GET    /api/jobs/{id}         → single job
 * POST   /api/jobs              → create (scraper only, requires X-API-Key)
 * PATCH  /api/jobs/{id}/status  → update status
 * PATCH  /api/jobs/{id}/notes   → update notes
 * PATCH  /api/jobs/{id}/pdfurl  → update resume PDF URL (scraper only)
 * DELETE /api/jobs/{id}         → delete
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    @PatchMapping("/{id}/pdfurl")
    public ResponseEntity<?> updatePdfUrl(
            @RequestAttribute("uid") String uid,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            return jobService.updateFields(uid, id, Map.of("resumePdfUrl", body.getOrDefault("resumePdfUrl", "")))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update PDF URL"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllJobs(
            @RequestAttribute("uid") String uid,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            List<Job> jobs = (search != null && !search.isBlank())
                    ? jobService.searchJobs(uid, search)
                    : jobService.getAllJobs(uid, status);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            log.error("getAllJobs failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch jobs"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestAttribute("uid") String uid) {
        try {
            return ResponseEntity.ok(jobService.getStats(uid));
        } catch (Exception e) {
            log.error("getStats failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch stats"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@RequestAttribute("uid") String uid, @PathVariable String id) {
        try {
            return jobService.getJobById(uid, id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch job"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestAttribute("uid") String uid, @RequestBody Job job) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(jobService.saveJob(uid, job));
        } catch (Exception e) {
            log.error("createJob failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save job"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @RequestAttribute("uid") String uid,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null) return ResponseEntity.badRequest().body(Map.of("error", "Missing status"));
            return jobService.updateStatus(uid, id, status)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update status"));
        }
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<?> updateNotes(
            @RequestAttribute("uid") String uid,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            return jobService.updateNotes(uid, id, body.get("notes"))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update notes"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@RequestAttribute("uid") String uid, @PathVariable String id) {
        try {
            return jobService.deleteJob(uid, id)
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete job"));
        }
    }
}
