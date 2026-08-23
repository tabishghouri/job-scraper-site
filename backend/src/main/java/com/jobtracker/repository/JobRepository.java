package com.jobtracker.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.jobtracker.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore data access layer. Every job lives under its owner's uid, never
 * in a shared collection, so isolation is structural rather than a filter
 * that could be forgotten on some query.
 *
 * All Firestore SDK calls are async (return ApiFuture<T>).
 * We call .get() to block and wait — simpler for a personal project.
 *
 * Firestore structure:
 *   firestore/
 *   └── users/{uid}/
 *       └── jobs/          ← collection
 *           ├── {docId}/   ← one document per job
 *           └── {docId}/
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JobRepository {

    private final Firestore firestore;

    private CollectionReference jobsCollection(String uid) {
        return firestore.collection("users").document(uid).collection("jobs");
    }

    /** Returns all jobs for uid, optionally filtered by status. Ordered newest first. */
    public List<Job> findAll(String uid, String status) throws ExecutionException, InterruptedException {
        CollectionReference col = jobsCollection(uid);
        Query query = (status != null && !status.isBlank())
                ? col.whereEqualTo("status", status)
                : col;
        ApiFuture<QuerySnapshot> future = query
                .orderBy("dateScraped", Query.Direction.DESCENDING)
                .get();
        return future.get().getDocuments().stream()
                .map(this::toJob)
                .collect(Collectors.toList());
    }

    /** Finds a single job by Firestore document ID, scoped to uid. */
    public Optional<Job> findById(String uid, String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = jobsCollection(uid).document(id).get().get();
        return doc.exists() ? Optional.of(toJob(doc)) : Optional.empty();
    }

    /** Checks if a job with this URL already exists for uid (deduplication). */
    public boolean existsByJobUrl(String uid, String jobUrl) throws ExecutionException, InterruptedException {
        return !jobsCollection(uid)
                .whereEqualTo("jobUrl", jobUrl)
                .limit(1).get().get().isEmpty();
    }

    /** Finds a job by URL for uid — returns it when a duplicate is detected. */
    public Optional<Job> findByJobUrl(String uid, String jobUrl) throws ExecutionException, InterruptedException {
        QuerySnapshot snap = jobsCollection(uid)
                .whereEqualTo("jobUrl", jobUrl).limit(1).get().get();
        return snap.isEmpty() ? Optional.empty() : Optional.of(toJob(snap.getDocuments().get(0)));
    }

    /** Creates a new Firestore document with auto-generated ID under uid. */
    public Job save(String uid, Job job) throws ExecutionException, InterruptedException {
        DocumentReference ref = jobsCollection(uid).add(toMap(job)).get();
        job.setId(ref.getId());
        log.info("Saved to Firestore: {} @ {} ({}) for uid {}", job.getJobTitle(), job.getCompanyName(), ref.getId(), uid);
        return job;
    }

    /**
     * Partially updates a document — only the provided fields are changed.
     * All other fields remain untouched.
     */
    public Optional<Job> updateFields(String uid, String id, Map<String, Object> fields)
            throws ExecutionException, InterruptedException {
        DocumentReference ref = jobsCollection(uid).document(id);
        if (!ref.get().get().exists()) return Optional.empty();
        ref.update(fields).get();
        return Optional.of(toJob(ref.get().get()));
    }

    /** Deletes a document for uid. Returns false if it didn't exist. */
    public boolean deleteById(String uid, String id) throws ExecutionException, InterruptedException {
        DocumentReference ref = jobsCollection(uid).document(id);
        if (!ref.get().get().exists()) return false;
        ref.delete().get();
        return true;
    }

    /**
     * In-memory keyword search across title and company name, scoped to uid.
     * Firestore doesn't support native full-text search — fine for personal use.
     */
    public List<Job> search(String uid, String keyword) throws ExecutionException, InterruptedException {
        String lower = keyword.toLowerCase();
        return findAll(uid, null).stream()
                .filter(j ->
                    (j.getJobTitle() != null && j.getJobTitle().toLowerCase().contains(lower)) ||
                    (j.getCompanyName() != null && j.getCompanyName().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    /** Counts uid's jobs by each status for the dashboard. */
    public Map<String, Long> getStats(String uid) throws ExecutionException, InterruptedException {
        List<Job> all = findAll(uid, null);
        return Map.of(
            "total",        (long) all.size(),
            "saved",        all.stream().filter(j -> "saved".equals(j.getStatus())).count(),
            "applied",      all.stream().filter(j -> "applied".equals(j.getStatus())).count(),
            "interviewing", all.stream().filter(j -> "interviewing".equals(j.getStatus())).count(),
            "rejected",     all.stream().filter(j -> "rejected".equals(j.getStatus())).count()
        );
    }

    // ── Firestore ↔ Job conversion ────────────────────────────────────────────

    private Job toJob(DocumentSnapshot d) {
        Job j = new Job();
        j.setId(d.getId());
        j.setJobUrl(d.getString("jobUrl"));
        j.setJobTitle(d.getString("jobTitle"));
        j.setCompanyName(d.getString("companyName"));
        j.setCompanyWebsite(d.getString("companyWebsite"));
        j.setLocation(d.getString("location"));
        j.setPostedAt(d.getString("postedAt"));
        j.setSalary(d.getString("salary"));
        j.setDescription(d.getString("description"));
        j.setNumApplications(d.getString("numApplications"));
        j.setJobPostLink(d.getString("jobPostLink"));
        j.setApplyUrl(d.getString("applyUrl"));
        j.setSource(d.getString("source"));
        j.setAtsKeywords(d.getString("atsKeywords"));
        j.setResumeFilename(d.getString("resumeFilename"));
        j.setResumePdf(d.getString("resumePdf"));
        j.setDateScraped(d.getString("dateScraped"));
        j.setStatus(d.getString("status") != null ? d.getString("status") : "saved");
        j.setNotes(d.getString("notes"));
        j.setResumePdfUrl(d.getString("resumePdfUrl"));
        return j;
    }

    private Map<String, Object> toMap(Job j) {
        Map<String, Object> m = new HashMap<>();
        putNN(m, "jobUrl",          j.getJobUrl());
        putNN(m, "jobTitle",        j.getJobTitle());
        putNN(m, "companyName",     j.getCompanyName());
        putNN(m, "companyWebsite",  j.getCompanyWebsite());
        putNN(m, "location",        j.getLocation());
        putNN(m, "postedAt",        j.getPostedAt());
        putNN(m, "salary",          j.getSalary());
        putNN(m, "description",     j.getDescription());
        putNN(m, "numApplications", j.getNumApplications());
        putNN(m, "jobPostLink",     j.getJobPostLink());
        putNN(m, "applyUrl",        j.getApplyUrl());
        putNN(m, "source",          j.getSource());
        putNN(m, "atsKeywords",     j.getAtsKeywords());
        putNN(m, "resumeFilename",  j.getResumeFilename());
        putNN(m, "resumePdf",       j.getResumePdf());
        putNN(m, "dateScraped",     j.getDateScraped());
        m.put("status", j.getStatus() != null ? j.getStatus() : "saved");
        putNN(m, "notes", j.getNotes());
        putNN(m, "resumePdfUrl", j.getResumePdfUrl());
        return m;
    }

    private void putNN(Map<String, Object> map, String key, Object val) {
        if (val != null) map.put(key, val);
    }
}
