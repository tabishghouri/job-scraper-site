package com.jobtracker.controller;

import com.jobtracker.config.SecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ScraperController — triggers the Python scraper as a subprocess
 * and streams its logs back to the frontend via Server-Sent Events (SSE).
 *
 * SSE is a one-way HTTP stream: the server pushes events to the client
 * in real time without the client needing to poll. Perfect for live logs.
 *
 * Endpoints:
 *   POST /api/scraper/run     → starts the scraper, streams logs via SSE
 *   GET  /api/scraper/status  → returns whether scraper is currently running
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraper")
public class ScraperController {

    private final SecurityConfig securityConfig;

    // Path to the Python executable in the scraper's venv
    // Windows: path to venv python.exe
    // Change this to match your actual scraper folder path
    @Value("${scraper.python.path:python}")
    private String pythonPath;

    @Value("${scraper.working.dir:../scraper}")
    private String scraperDir;

    @Value("${scraper.script:main.py}")
    private String scraperScript;

    // Track if scraper is running to prevent concurrent runs
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Thread pool for running the scraper subprocess
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * GET /api/scraper/status
     * Returns whether the scraper is currently running.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "running", isRunning.get(),
            "message", isRunning.get() ? "Scraper is running" : "Scraper is idle"
        ));
    }

    /**
     * POST /api/scraper/run
     *
     * Starts the Python scraper as a subprocess and streams its stdout/stderr
     * back to the client as Server-Sent Events.
     *
     * Each log line becomes an SSE event the frontend can display in real time.
     * When the process finishes, a "done" event is sent.
     *
     * Protected by X-API-Key — same key the Python scraper uses.
     * The frontend sends this key from an env variable.
     */
    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runScraper(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam(value = "maxJobs", required = false) Integer maxJobs) {

        final Integer clampedMaxJobs = maxJobs == null ? null : Math.max(0, Math.min(50, maxJobs));

        // Validate API key
        if (!securityConfig.scraperApiKey.equals(apiKey)) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("Unauthorized"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        // Prevent concurrent runs
        if (!isRunning.compareAndSet(false, true)) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("Scraper is already running"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        // SseEmitter with 30 minute timeout (scraper can take a while)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        executor.submit(() -> {
            Process process = null;
            try {
                // Build the command
                java.util.List<String> command = new java.util.ArrayList<>(java.util.List.of(pythonPath, scraperScript));
                if (clampedMaxJobs != null) {
                    command.add("--max-jobs");
                    command.add(String.valueOf(clampedMaxJobs));
                }
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new java.io.File(scraperDir));
                pb.redirectErrorStream(true);  // merge stderr into stdout

                log.info("Starting scraper: {} in {}", command, scraperDir);
                emitter.send(SseEmitter.event()
                    .name("log")
                    .data("🚀 Starting scraper..."));

                process = pb.start();

                // Stream output line by line
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("Scraper: {}", line);
                        emitter.send(SseEmitter.event()
                            .name("log")
                            .data(line));
                    }
                }

                int exitCode = process.waitFor();
                String doneMsg = exitCode == 0
                    ? "✅ Scraper completed successfully"
                    : "❌ Scraper exited with code " + exitCode;

                emitter.send(SseEmitter.event().name("done").data(doneMsg));
                emitter.complete();
                log.info("Scraper finished with exit code {}", exitCode);

            } catch (Exception e) {
                log.error("Scraper subprocess error", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("Error: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ignored) {}
            } finally {
                isRunning.set(false);
                if (process != null) process.destroy();
            }
        });

        emitter.onCompletion(() -> isRunning.set(false));
        emitter.onTimeout(() -> {
            isRunning.set(false);
            log.warn("Scraper SSE timed out");
        });

        return emitter;
    }
}