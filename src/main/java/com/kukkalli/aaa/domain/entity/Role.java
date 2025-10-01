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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_roles_code", columnNames = "code")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g., ROLE_ADMIN, ROLE_USER */
    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String code;

    @NotBlank
    @Size(max = 128)
    @Column(nullable = false, length = 128)
    private String name;

    @Size(max = 512)
    @Column(length = 512)
    private String description;

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

    // --- Relations ---

    /** Inverse side of User.roles (user_roles join table). */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new LinkedHashSet<>();

    /** Role ↔ Permission (role_permissions join table). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_role_permissions_role")),
            inverseJoinColumns = @JoinColumn(name = "permission_id", foreignKey = @ForeignKey(name = "fk_role_permissions_permission"))
    )
    @Builder.Default
    private Set<Permission> permissions = new LinkedHashSet<>();

    // --- Convenience helpers ---
    public void addPermission(Permission permission) {
        permissions.add(permission);
    }
    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    // Equality based on business key "code"
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return Objects.equals(code, other.code);
    }
    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
