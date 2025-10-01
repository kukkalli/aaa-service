package com.kukkalli.aaa.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash")
        },
        indexes = {
                @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_expires", columnList = "expires_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner of this token. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_refresh_tokens_user"),
            nullable = false)
    private User user;

    /** SHA-256 hash of the opaque refresh token string. */
    @NotBlank
    @Size(max = 64)
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @CreatedDate
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // --- Convenience methods ---
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    // Equality based on unique tokenHash
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken other)) return false;
        return Objects.equals(tokenHash, other.tokenHash);
    }
    @Override
    public int hashCode() {
        return Objects.hash(tokenHash);
    }
}
