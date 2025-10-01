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
@Table(name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_permissions_code", columnNames = "code")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g., user.read, user.write, audit.read */
    @NotBlank
    @Size(max = 128)
    @Column(nullable = false, length = 128)
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

    /** Inverse side of Role.permissions (role_permissions join table). */
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    // --- Convenience ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission other)) return false;
        return Objects.equals(code, other.code);
    }
    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
