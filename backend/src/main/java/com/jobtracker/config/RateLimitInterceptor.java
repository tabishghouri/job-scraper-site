package com.jobtracker.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Objects;

/**
 * Rate limits every API request at N requests/minute per IP.
 * Uses Token Bucket algorithm: bucket starts full, each request consumes one
 * token, tokens refill at a fixed rate. Empty bucket = 429 Too Many Requests.
 *
 * Runs after AuthInterceptor (order 2 vs 1), which handles who's allowed to
 * call what at all — this only throttles volume.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final CacheManager cacheManager;

    @Value("${app.rate-limit.requests-per-minute:30}")
    private int requestsPerMinute;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**").order(2);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String clientIp = getClientIp(request);
        Bucket bucket   = getOrCreateBucket(clientIp);

        long remaining = bucket.getAvailableTokens();
        response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.addHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));

        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("Rate limit exceeded for IP: {}", clientIp);
        response.setStatus(429);
        response.setContentType("application/json");
        response.addHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again in 60 seconds.\"}");
        return false;
    }

    private Bucket getOrCreateBucket(String ip) {
        Cache cache = cacheManager.getCache("rate-limit-buckets");
        return Objects.requireNonNull(cache).get(ip, this::createBucket);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
