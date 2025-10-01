package com.kukkalli.aaa.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "api_clients",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_api_clients_client_id", columnNames = "client_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public identifier used by the client. */
    @NotBlank
    @Size(max = 128)
    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    /** Store only a one-way hash (e.g., BCrypt/Argon2) of the secret. */
    @NotBlank
    @Size(max = 255)
    @Column(name = "client_secret_hash", nullable = false, length = 255)
    private String clientSecretHash;

    /** Human-friendly name/label for admin consoles. */
    @NotBlank
    @Size(max = 191)
    @Column(nullable = false, length = 191)
    private String name;

    /** Space- or comma-delimited scopes (keep simple; can normalize later). */
    @Size(max = 512)
    @Column(length = 512)
    private String scopes;

    /** CSV/CIDR list of allowed IPs (optional). */
    @Size(max = 1024)
    @Column(name = "allowed_ips", length = 1024)
    private String allowedIps;

    @Column(nullable = false)
    private boolean enabled = true;

    // --- Auditing ---
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    // Equality based on business key "clientId"
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiClient other)) return false;
        return Objects.equals(clientId, other.clientId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
