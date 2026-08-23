package com.jobtracker.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.jobtracker.model.Job;
import com.jobtracker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository repo;
    private final Bucket bucket;

    private static final List<String> VALID_STATUSES =
            List.of("saved", "applied", "interviewing", "rejected");

    public Optional<Job> updateFields(String uid, String id, Map<String, Object> fields)
        throws ExecutionException, InterruptedException {
        return repo.updateFields(uid, id, fields);
    }

    public List<Job> getAllJobs(String uid, String status) throws ExecutionException, InterruptedException {
        return repo.findAll(uid, status);
    }

    public Optional<Job> getJobById(String uid, String id) throws ExecutionException, InterruptedException {
        return repo.findById(uid, id);
    }

    public List<Job> searchJobs(String uid, String keyword) throws ExecutionException, InterruptedException {
        return repo.search(uid, keyword);
    }

    public Job saveJob(String uid, Job job) throws ExecutionException, InterruptedException {
        if (job.getJobUrl() != null && repo.existsByJobUrl(uid, job.getJobUrl())) {
            log.info("Duplicate skipped: {}", job.getJobUrl());
            return repo.findByJobUrl(uid, job.getJobUrl()).orElse(job);
        }
        if (job.getStatus() == null || job.getStatus().isBlank()) {
            job.setStatus("saved");
        }
        return repo.save(uid, job);
    }

    public Optional<Job> updateStatus(String uid, String id, String status)
            throws ExecutionException, InterruptedException {
        if (!VALID_STATUSES.contains(status.toLowerCase()))
            throw new IllegalArgumentException("Invalid status: " + status);
        return repo.updateFields(uid, id, Map.of("status", status.toLowerCase()));
    }

    public Optional<Job> updateNotes(String uid, String id, String notes)
            throws ExecutionException, InterruptedException {
        return repo.updateFields(uid, id, Map.of("notes", notes != null ? notes : ""));
    }

    /**
     * Deletes a job and its tailored resume from Firebase Storage.
     * Only removes the cloud copy — the scraper's local .tex/.pdf files and
     * SQLite record are left alone, so a deleted job is still recoverable locally.
     */
    public boolean deleteJob(String uid, String id) throws ExecutionException, InterruptedException {
        repo.findById(uid, id).ifPresent(job -> deleteResumeFromStorage(uid, job.getResumePdf()));
        return repo.deleteById(uid, id);
    }

    private void deleteResumeFromStorage(String uid, String resumePdf) {
        if (resumePdf == null || resumePdf.isBlank()) return;
        try {
            Blob blob = bucket.get("resumes/" + uid + "/" + resumePdf);
            if (blob != null && blob.delete()) {
                log.info("Deleted resume from Storage: resumes/{}/{}", uid, resumePdf);
            }
        } catch (Exception e) {
            log.warn("Failed to delete resume from Storage for '{}' (job delete continues): {}", resumePdf, e.getMessage());
        }
    }

    public Map<String, Long> getStats(String uid) throws ExecutionException, InterruptedException {
        return repo.getStats(uid);
    }
}
