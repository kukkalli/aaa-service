package com.kukkalli.aaa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Clock;
import java.util.Optional;

@Configuration
public class AuditConfig {
    /**
     * Supplies the "current auditor" for JPA auditing annotations
     * like @CreatedBy and @LastModifiedBy.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof UserDetails userDetails) {
                        return userDetails.getUsername();
                    }
                    String name = auth.getName();
                    return (name != null && !"anonymousUser".equalsIgnoreCase(name)) ? name : null;
                })
                .or(() -> Optional.of("system")); // fallback for system tasks / unauthenticated contexts
    }

    /**
     * Single source of time for the app (UTC).
     * Inject Clock where you need "now()" to be testable.
     */
    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }
}
