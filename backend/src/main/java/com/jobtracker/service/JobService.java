package com.jobtracker.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.jobtracker.model.Job;
import com.jobtracker.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
        return repo.updateFields(uid, id, fields).map(job -> attachSignedUrl(uid, job));
    }

    public List<Job> getAllJobs(String uid, String status) throws ExecutionException, InterruptedException {
        return repo.findAll(uid, status).stream().map(job -> attachSignedUrl(uid, job)).toList();
    }

    public Optional<Job> getJobById(String uid, String id) throws ExecutionException, InterruptedException {
        return repo.findById(uid, id).map(job -> attachSignedUrl(uid, job));
    }

    public List<Job> searchJobs(String uid, String keyword) throws ExecutionException, InterruptedException {
        return repo.search(uid, keyword).stream().map(job -> attachSignedUrl(uid, job)).toList();
    }

    public Job saveJob(String uid, Job job) throws ExecutionException, InterruptedException {
        if (job.getJobUrl() != null && repo.existsByJobUrl(uid, job.getJobUrl())) {
            log.info("Duplicate skipped: {}", job.getJobUrl());
            return attachSignedUrl(uid, repo.findByJobUrl(uid, job.getJobUrl()).orElse(job));
        }
        if (job.getStatus() == null || job.getStatus().isBlank()) {
            job.setStatus("saved");
        }
        return attachSignedUrl(uid, repo.save(uid, job));
    }

    public Optional<Job> updateStatus(String uid, String id, String status)
            throws ExecutionException, InterruptedException {
        if (!VALID_STATUSES.contains(status.toLowerCase()))
            throw new IllegalArgumentException("Invalid status: " + status);
        return repo.updateFields(uid, id, Map.of("status", status.toLowerCase())).map(job -> attachSignedUrl(uid, job));
    }

    public Optional<Job> updateNotes(String uid, String id, String notes)
            throws ExecutionException, InterruptedException {
        return repo.updateFields(uid, id, Map.of("notes", notes != null ? notes : "")).map(job -> attachSignedUrl(uid, job));
    }

    /** Updates which resume PDF a job points at (just the filename — the URL is computed on read). */
    public Optional<Job> updateResumeFilename(String uid, String id, String resumePdf)
            throws ExecutionException, InterruptedException {
        return repo.updateFields(uid, id, Map.of("resumePdf", resumePdf != null ? resumePdf : "")).map(job -> attachSignedUrl(uid, job));
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

    /**
     * Resumes are stored privately now (not made public at upload time), so the
     * download URL is computed fresh on every read instead of persisted —
     * nothing in Firestore holds a long-lived link to another user's resume.
     */
    private Job attachSignedUrl(String uid, Job job) {
        if (job.getResumePdf() == null || job.getResumePdf().isBlank()) return job;
        try {
            Blob blob = bucket.get("resumes/" + uid + "/" + job.getResumePdf());
            if (blob != null) {
                job.setResumePdfUrl(blob.signUrl(1, TimeUnit.HOURS, Storage.SignUrlOption.withV4Signature()).toString());
            }
        } catch (Exception e) {
            log.warn("Failed to sign resume URL for '{}': {}", job.getResumePdf(), e.getMessage());
        }
        return job;
    }

    public Map<String, Long> getStats(String uid) throws ExecutionException, InterruptedException {
        return repo.getStats(uid);
    }
}
