package com.kukkalli.aaa.service;

import com.kukkalli.aaa.domain.entity.ApiClient;
import com.kukkalli.aaa.domain.entity.AuditLog;
import com.kukkalli.aaa.domain.entity.User;
import com.kukkalli.aaa.domain.repository.ApiClientRepository;
import com.kukkalli.aaa.domain.repository.AuditLogRepository;
import com.kukkalli.aaa.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditRepo;
    private final UserRepository userRepo;
    private final ApiClientRepository apiClientRepo;
    private final Clock clock;

    // ---------------------------------------------------------------------
    // Public API (simple entry points)
    // ---------------------------------------------------------------------

    /** Minimal: just action and optional details; pulls an actor from SecurityContext (if any). */
    @Async
    @Transactional
    public void audit(String action, Map<String, Object> details) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        save(build(action, null, null, null, null, null, details, auth));
    }

    /** From a servlet request (captures IP / UA / X-Request-Id) + SecurityContext actor. */
    @Async
    @Transactional
    public void audit(String action, HttpServletRequest req, Map<String, Object> details) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String ip = clientIp(req);
        String ua = userAgent(req);
        String rid = requestId(req);
        save(build(action, null, null, rid, ip, ua, details, auth));
    }

    /** Explicit actor: user entity. Useful in services when you already loaded the user. */
    @Async
    @Transactional
    public void auditForUser(String action, User user, HttpServletRequest req, Map<String, Object> details) {
        String ip = clientIp(req);
        String ua = userAgent(req);
        String rid = requestId(req);
        save(build(action, user, null, rid, ip, ua, details, null));
    }

    /** Explicit actor: by username (resolved to User if present). */
    @Async
    @Transactional
    public void auditForUsername(String action, String username, HttpServletRequest req, Map<String, Object> details) {
        User user = userRepo.findByUsernameIgnoreCase(username).orElse(null);
        String ip = clientIp(req);
        String ua = userAgent(req);
        String rid = requestId(req);
        save(build(action, user, null, rid, ip, ua, details, null));
    }

    /** Explicit actor: API client (M2M). */
    @Async
    @Transactional
    public void auditForClient(String action, ApiClient client, HttpServletRequest req, Map<String, Object> details) {
        String ip = clientIp(req);
        String ua = userAgent(req);
        String rid = requestId(req);
        save(build(action, null, client, rid, ip, ua, details, null));
    }

    /** System-initiated events without a user/client (maintenance jobs, etc.). */
    @Async
    @Transactional
    public void auditSystem(String action, Map<String, Object> details) {
        save(build(action, null, null, null, null, null, details, null));
    }

    // ---------------------------------------------------------------------
    // Builder + helpers
    // ---------------------------------------------------------------------

    private AuditLog build(String action,
                           User user,
                           ApiClient client,
                           String requestId,
                           String ip,
                           String ua,
                           Map<String, Object> details,
                           Authentication fallbackAuth) {

        // If no explicit user/client passed, try resolving from SecurityContext
        if (user == null && client == null && fallbackAuth != null && fallbackAuth.isAuthenticated()) {
            String principalName = fallbackAuth.getName();
            // Try to map the principalName to a User; if not found, leave actor null (system/M2M).
            user = userRepo.findByUsernameIgnoreCase(principalName).orElse(null);
            // If you maintain distinct principal prefixes for M2M (e.g., "client:<id>"),
            // you could resolve ApiClient here as well.
        }

        return AuditLog.builder()
                .occurredAt(Instant.now(clock))
                .actorUser(user)
                .actorClient(client)
                .action(Objects.requireNonNull(action, "action must not be null"))
                .requestId(defaultIfBlank(requestId, UUID.randomUUID().toString()))
                .ipAddress(ip)
                .userAgent(ua)
                .details(copy(details))
                .build();
    }

    private void save(AuditLog log) {
        auditRepo.save(log);
    }

    // ---------------------------------------------------------------------
    // HTTP extraction helpers
    // ---------------------------------------------------------------------

    private static String requestId(HttpServletRequest http) {
        String rid = http.getHeader("X-Request-Id");
        return StringUtils.hasText(rid) ? rid : UUID.randomUUID().toString();
    }

    private static String clientIp(HttpServletRequest http) {
        String h = http.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(h)) {
            int comma = h.indexOf(',');
            return comma > 0 ? h.substring(0, comma).trim() : h.trim();
        }
        return http.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest http) {
        String ua = http.getHeader(HttpHeaders.USER_AGENT);
        return ua != null ? ua : "unknown";
    }

    private static String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static Map<String, Object> copy(Map<String, Object> details) {
        return details == null ? null : new LinkedHashMap<>(details);
    }
}
