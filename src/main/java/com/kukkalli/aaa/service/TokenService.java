package com.kukkalli.aaa.service;

import com.kukkalli.aaa.domain.entity.RefreshToken;
import com.kukkalli.aaa.domain.entity.User;
import com.kukkalli.aaa.domain.repository.RefreshTokenRepository;
import com.kukkalli.aaa.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepo;
    private final UserRepository userRepo;
    private final Duration refreshTtl;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(RefreshTokenRepository refreshTokenRepo,
                        UserRepository userRepo,
                        @Value("${security.jwt.refresh-token.ttl:P7D}") Duration refreshTtl,
                        Clock clock) {
        this.refreshTokenRepo = Objects.requireNonNull(refreshTokenRepo);
        this.userRepo = Objects.requireNonNull(userRepo);
        this.refreshTtl = Objects.requireNonNull(refreshTtl);
        this.clock = Objects.requireNonNull(clock);
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Issues a new opaque refresh token for a user and persists its hash.
     * @return the raw refresh token string (caller must return it to a client).
     */
    @Transactional
    public String issue(User user, String ipAddress, String userAgent) {
        Instant now = clock.instant();
        String raw = generateOpaqueToken();          // raw to give to a client
        String hash = sha256(raw);                   // only hash stored

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .issuedAt(now)
                .expiresAt(now.plus(refreshTtl))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        refreshTokenRepo.save(entity);
        return raw;
    }

    /**
     * Validates a refresh token and returns its owning user if valid (not revoked/expired).
     * Does NOT rotate or mutate state.
     */
    @Transactional(readOnly = true)
    public Optional<User> validateAndGetUser(String rawToken) {
        String hash = sha256(rawToken);
        return refreshTokenRepo.findByTokenHash(hash)
                .filter(rt -> !rt.isRevoked() && !rt.isExpired())
                .map(RefreshToken::getUser);
    }

    /**
     * Rotates (single-use) a refresh token:
     * - marks the old one revoked
     * - issues a brand new one and returns it
     */
    @Transactional
    public Optional<String> rotate(String rawToken, String ipAddress, String userAgent) {
        String oldHash = sha256(rawToken);
        return refreshTokenRepo.findByTokenHash(oldHash)
                .filter(rt -> !rt.isRevoked() && !rt.isExpired())
                .map(rt -> {
                    // revoke old
                    rt.revoke();
                    refreshTokenRepo.save(rt);
                    // issue new
                    return issue(rt.getUser(), ipAddress, userAgent);
                });
    }

    /**
     * Revokes ALL refresh tokens for a user (e.g., on password change or logout-all).
     * @return number of tokens deleted
     */
    @Transactional
    public long revokeAllForUser(String username) {
        var user = userRepo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return refreshTokenRepo.deleteByUser(user);
    }

    /**
     * Deletes expired tokens (housekeeping job).
     * @return number of tokens removed
     */
    @Transactional
    public long cleanupExpired() {
        return refreshTokenRepo.deleteByExpiresAtBefore(clock.instant());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String generateOpaqueToken() {
        // 256-bit random, Base64 URL-safe without padding, prefixed with a short version for UX
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // optional: short prefix for quick recognition
        return "rt_" + body;
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }
}
