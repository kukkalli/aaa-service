package com.kukkalli.aaa.web.controller;

import com.kukkalli.aaa.service.AuthService;
import com.kukkalli.aaa.web.dto.AuthRequest;
import com.kukkalli.aaa.web.dto.AuthResponse;
import com.kukkalli.aaa.web.dto.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                              HttpServletRequest http) {
        AuthResponse response = authService.login(
                request.usernameOrEmail(),
                request.password(),
                http
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                     HttpServletRequest http) {
        Optional<AuthResponse> refreshed = authService.refresh(
                request.refreshToken(),
                http
        );

        return refreshed.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(
                        Map.of("error", "invalid_refresh_token")
                ));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutAll(Principal principal,
                                                         HttpServletRequest http) {
        String username = principal.getName();

        long count = authService.logoutAll(username, http);
        return ResponseEntity.ok(Map.of("revokedCount", count));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String requestId(HttpServletRequest http) {
        String rid = http.getHeader("X-Request-Id");
        return StringUtils.hasText(rid) ? rid : UUID.randomUUID().toString();
        // If you use Spring Cloud Sleuth / Micrometer Tracing, you could also
        // read B3/traceparent headers here for correlation.
    }

    private static String clientIp(HttpServletRequest http) {
        String h = http.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(h)) {
            // take the first IP in the list
            int comma = h.indexOf(',');
            return comma > 0 ? h.substring(0, comma).trim() : h.trim();
        }
        return http.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest http) {
        String ua = http.getHeader(HttpHeaders.USER_AGENT);
        return ua != null ? ua : "unknown";
    }
}
