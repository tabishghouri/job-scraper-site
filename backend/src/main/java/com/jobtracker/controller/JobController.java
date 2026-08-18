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
 *
 * GET    /api/jobs              → all jobs (?status=applied, ?search=shopify)
 * GET    /api/jobs/stats        → dashboard counts
 * GET    /api/jobs/{id}         → single job
 * POST   /api/jobs              → create (scraper only, requires X-API-Key)
 * PATCH  /api/jobs/{id}/status  → update status
 * PATCH  /api/jobs/{id}/notes   → update notes
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
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            return jobService.updateFields(id, Map.of("resumePdfUrl", body.getOrDefault("resumePdfUrl", "")))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update PDF URL"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            List<Job> jobs = (search != null && !search.isBlank())
                    ? jobService.searchJobs(search)
                    : jobService.getAllJobs(status);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            log.error("getAllJobs failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch jobs"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            return ResponseEntity.ok(jobService.getStats());
        } catch (Exception e) {
            log.error("getStats failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch stats"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable String id) {
        try {
            return jobService.getJobById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch job"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody Job job) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(jobService.saveJob(job));
        } catch (Exception e) {
            log.error("createJob failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save job"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null) return ResponseEntity.badRequest().body(Map.of("error", "Missing status"));
            return jobService.updateStatus(id, status)
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
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            return jobService.updateNotes(id, body.get("notes"))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update notes"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable String id) {
        try {
            return jobService.deleteJob(id)
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete job"));
        }
    }
}
