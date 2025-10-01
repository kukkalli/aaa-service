package com.kukkalli.aaa.jobs;

import com.kukkalli.aaa.service.AuditService;
import com.kukkalli.aaa.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Nightly maintenance jobs.
 * Make sure @EnableScheduling is present (it is, in AaaServiceApplication).
 */
@Component
@RequiredArgsConstructor
public class HousekeepingJobs {

    private final TokenService tokenService;
    private final AuditService auditService;

    /**
     * Clean up expired refresh tokens hourly (tweak as desired).
     * Cron format: second minute hour day-of-month month day-of-week
     * "0 0 * * * *" = at minute 0 of every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredRefreshTokens() {
        long removed = tokenService.cleanupExpired();
        auditService.auditSystem("REFRESH_TOKEN_CLEANUP", Map.of("removed", removed));
    }
}
