package com.jobtracker.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.jobtracker.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Verifies who's calling every /api/** request and stashes their uid on the
 * request for controllers to read via @RequestAttribute("uid").
 *
 * Two mechanisms depending on who's calling:
 *  - The scraper (a human's local Python script) can't do OAuth refresh, so it
 *    sends a personal X-API-Key header instead — POST /api/jobs,
 *    PATCH .../pdfurl, and POST /api/scraper/run.
 *  - Everything else is the logged-in browser, sending a Firebase ID token as
 *    Authorization: Bearer <token>.
 *
 * Runs before RateLimitInterceptor (order 1 vs 2) so unauthenticated requests
 * fail fast instead of spending a rate-limit token first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final ApiKeyService apiKeyService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/**").order(1);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        String path = request.getRequestURI();

        boolean isScraperRoute =
                ("POST".equals(method) && "/api/jobs".equals(path)) ||
                ("PATCH".equals(method) && path.endsWith("/pdfurl")) ||
                ("POST".equals(method) && "/api/scraper/run".equals(path));

        String uid = isScraperRoute
                ? apiKeyService.resolveUid(request.getHeader("X-API-Key"))
                : verifyBearerToken(request.getHeader("Authorization"));

        if (uid == null) {
            log.warn("Unauthorized {} {} from {}", method, path, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }

        request.setAttribute("uid", uid);
        return true;
    }

    private String verifyBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(authHeader.substring(7));
            return decoded.getUid();
        } catch (Exception e) {
            return null;
        }
    }
}
