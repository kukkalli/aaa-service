package com.kukkalli.aaa.security.jwt;

import com.kukkalli.aaa.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/actuator/health", "/actuator/info",
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs/**", "/openapi/**",
            "/api/v1/auth/**"
    );

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsService userDetailsService,
                                   AuditService auditService) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.userDetailsService = Objects.requireNonNull(userDetailsService);
        this.auditService = Objects.requireNonNull(auditService);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        for (String pattern : PUBLIC_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();

            boolean valid = tokenProvider.validate(token);
            if (!valid) {
                // audit once for invalid token (don’t log the token itself)
                auditService.audit("AUTH_TOKEN_INVALID",
                        java.util.Map.of("reason", "signature/expiry", "path", request.getRequestURI()));
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                tokenProvider.extractUsername(token).ifPresent(username -> {
                    try {
                        UserDetails user = userDetailsService.loadUserByUsername(username);
                        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception ex) {
                        // If the user no longer exists or is disabled, clear context and audit once
                        SecurityContextHolder.clearContext();
                        auditService.audit("AUTH_TOKEN_USERLOAD_FAIL",
                                java.util.Map.of("path", request.getRequestURI(), "username", username));
                    }
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
