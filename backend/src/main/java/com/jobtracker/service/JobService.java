package com.jobtracker.service;

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

    private static final List<String> VALID_STATUSES =
            List.of("saved", "applied", "interviewing", "rejected");

    public Optional<Job> updateFields(String id, Map<String, Object> fields)
        throws ExecutionException, InterruptedException {
        return repo.updateFields(id, fields);
    }
    
    public List<Job> getAllJobs(String status) throws ExecutionException, InterruptedException {
        return repo.findAll(status);
    }

    public Optional<Job> getJobById(String id) throws ExecutionException, InterruptedException {
        return repo.findById(id);
    }

    public List<Job> searchJobs(String keyword) throws ExecutionException, InterruptedException {
        return repo.search(keyword);
    }

    public Job saveJob(Job job) throws ExecutionException, InterruptedException {
        if (job.getJobUrl() != null && repo.existsByJobUrl(job.getJobUrl())) {
            log.info("Duplicate skipped: {}", job.getJobUrl());
            return repo.findByJobUrl(job.getJobUrl()).orElse(job);
        }
        if (job.getStatus() == null || job.getStatus().isBlank()) {
            job.setStatus("saved");
        }
        return repo.save(job);
    }

    public Optional<Job> updateStatus(String id, String status)
            throws ExecutionException, InterruptedException {
        if (!VALID_STATUSES.contains(status.toLowerCase()))
            throw new IllegalArgumentException("Invalid status: " + status);
        return repo.updateFields(id, Map.of("status", status.toLowerCase()));
    }

    public Optional<Job> updateNotes(String id, String notes)
            throws ExecutionException, InterruptedException {
        return repo.updateFields(id, Map.of("notes", notes != null ? notes : ""));
    }

    public boolean deleteJob(String id) throws ExecutionException, InterruptedException {
        return repo.deleteById(id);
    }

    public Map<String, Long> getStats() throws ExecutionException, InterruptedException {
        return repo.getStats();
    }
}