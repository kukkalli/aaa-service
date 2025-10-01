package com.kukkalli.aaa.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Persisted audit events for security-relevant actions.
 * Matches Flyway V1__init_schema.sql (audit_log table).
 */
@Entity
@Table(name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_log_time", columnList = "occurred_at"),
                @Index(name = "idx_audit_log_action", columnList = "action"),
                @Index(name = "idx_audit_log_actor_user", columnList = "actor_user_id"),
                @Index(name = "idx_audit_log_actor_client", columnList = "actor_client_id")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Event time in UTC (distinct from created_at which is insertion time). */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** User who performed the action (nullable for system/M2M events). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "actor_user_id",
            foreignKey = @ForeignKey(name = "fk_audit_log_actor_user")
    )
    private User actorUser;

    /** API client (M2M) that performed the action (nullable). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "actor_client_id",
            foreignKey = @ForeignKey(name = "fk_audit_log_actor_client")
    )
    private ApiClient actorClient;

    /** Action code: e.g., AUTH_LOGIN, USER_CREATE, ROLE_ASSIGN, etc. */
    @NotBlank
    @Size(max = 128)
    @Column(name = "action", nullable = false, length = 128)
    private String action;

    /** Optional target entity type (USER, ROLE, PERMISSION, CLIENT, etc.). */
    @Size(max = 128)
    @Column(name = "target_type", length = 128)
    private String targetType;

    /** Optional business identifier of the target (string form). */
    @Size(max = 191)
    @Column(name = "target_id", length = 191)
    private String targetId;

    /** Correlation / request ID for tracing. */
    @Size(max = 64)
    @Column(name = "request_id", length = 64)
    private String requestId;

    @Size(max = 64)
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Size(max = 255)
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** Arbitrary structured payload persisted as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "json")
    private Map<String, Object> details;

    /** Row insertion time (DB default CURRENT_TIMESTAMP). */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp default current_timestamp")
    private Instant createdAt;
}
