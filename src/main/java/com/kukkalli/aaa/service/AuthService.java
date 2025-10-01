package com.kukkalli.aaa.service;

import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.entity.User;
import com.kukkalli.aaa.domain.repository.UserRepository;
import com.kukkalli.aaa.security.jwt.JwtTokenProvider;
import com.kukkalli.aaa.web.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final Clock clock;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenService tokenService,
                       Clock clock,
                       AuditService auditService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider);
        this.tokenService = Objects.requireNonNull(tokenService);
        this.clock = Objects.requireNonNull(clock);
        this.auditService = Objects.requireNonNull(auditService);
    }

    // ---------------------------------------------------------------------
    // Login with username/email + password
    // ---------------------------------------------------------------------
    @Transactional
    public AuthResponse login(String usernameOrEmail,
                              String rawPassword,
                              HttpServletRequest request) {

        // Lookup by username first, then email
        User user = userRepository.findOneWithRolesByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(usernameOrEmail)
                        .flatMap(u -> userRepository.findOneWithRolesByUsernameIgnoreCase(u.getUsername())))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            // audit failed attempt (captures IP/UA/Request-ID from request)
            auditService.auditForUser("AUTH_LOGIN_FAIL", user, request,
                    Map.of("reason", "bad_password"));
            throw new BadCredentialsException("Invalid credentials");
        }

        // Build Spring Security principal for JWT authority embedding
        var authorities = toAuthorities(user);
        var principal = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();

        // Access token
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        var exp = jwtTokenProvider.parseClaims(accessToken)
                .map(c -> c.getExpirationTime().toInstant())
                .orElse(Instant.now(clock).plusSeconds(900)); // fallback ~15m

        // Refresh token (we store only hash; raw token returned to client)
        String refreshToken = tokenService.issue(
                user,
                clientIp(request),
                userAgent(request)
        );

        // audit success
        auditService.auditForUser("AUTH_LOGIN", user, request,
                Map.of("username", user.getUsername()));

        return new AuthResponse(accessToken, refreshToken, exp);
    }

    // ---------------------------------------------------------------------
    // Refresh: validate & rotate refresh token, issue new access token
    // ---------------------------------------------------------------------
    @Transactional
    public Optional<AuthResponse> refresh(String rawRefreshToken,
                                          HttpServletRequest request) {

        return tokenService.validateAndGetUser(rawRefreshToken).flatMap(user -> {
            // Rebuild authorities for new access token
            var authorities = toAuthorities(user);
            var principal = org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPasswordHash())
                    .authorities(authorities)
                    .accountExpired(!user.isAccountNonExpired())
                    .accountLocked(!user.isAccountNonLocked())
                    .credentialsExpired(!user.isCredentialsNonExpired())
                    .disabled(!user.isEnabled())
                    .build();

            String accessToken = jwtTokenProvider.generateAccessToken(principal);
            var exp = jwtTokenProvider.parseClaims(accessToken)
                    .map(c -> c.getExpirationTime().toInstant())
                    .orElse(Instant.now(clock).plusSeconds(900));

            // rotate refresh token (single-use)
            var rotated = tokenService.rotate(
                    rawRefreshToken,
                    clientIp(request),
                    userAgent(request)
            );

            rotated.ifPresent(newRt -> auditService.auditForUser("AUTH_REFRESH", user, request,
                    Map.of("rotated", true)));

            return rotated.map(newRt -> new AuthResponse(accessToken, newRt, exp));
        });
    }

    // ---------------------------------------------------------------------
    // Logout-all: revoke all refresh tokens for the user
    // ---------------------------------------------------------------------
    @Transactional
    public long logoutAll(String username, HttpServletRequest request) {
        long count = tokenService.revokeAllForUser(username);

        auditService.auditForUsername("AUTH_LOGOUT_ALL", username, request,
                Map.of("revoked_count", count));

        return count;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Set<SimpleGrantedAuthority> toAuthorities(User user) {
        return user.getRoles().stream()
                .flatMap(role -> {
                    var roleAuth = new SimpleGrantedAuthority(role.getCode()); // ROLE_*
                    var permAuths = role.getPermissions().stream()
                            .map(Permission::getCode)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet());
                    permAuths.add(roleAuth);
                    return permAuths.stream();
                })
                .collect(Collectors.toSet());
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
}
