package com.kukkalli.aaa.domain.entity;

import com.kukkalli.aaa.domain.repository.PermissionRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class PermissionEntityIT extends SpringBootITBase {

  @Autowired private PermissionRepository repo;
  @Autowired private EntityManager em;

  @Test
  @DisplayName("CRUD round-trip persists and loads all main fields")
  void crud_roundtrip() {
    var p = Permission.builder()
        .code("user.read")
        .name("Read Users")
        .description("Allows reading user profiles")
        .build();

    var saved = repo.save(p);
    assertThat(saved.getId()).isNotNull();

    em.flush();
    em.clear();

    var found = repo.findById(saved.getId()).orElseThrow();
    assertThat(found.getCode()).isEqualTo("user.read");
    assertThat(found.getName()).isEqualTo("Read Users");
    assertThat(found.getDescription()).isEqualTo("Allows reading user profiles");
  }

  @Test
  @DisplayName("Unique constraint on code is enforced (case-insensitive repo check + DB unique key)")
  void unique_code_constraint() {
    var a = Permission.builder().code("audit.read").name("Audit Read").build();
    repo.save(a);

    // Application-level check via repository.existsByCodeIgnoreCase
    assertThat(repo.existsByCodeIgnoreCase("AUDIT.READ")).isTrue();

    // DB-level uniqueness (same exact code) -> expect persistence exception on flush
    var dup = Permission.builder().code("audit.read").name("Duplicate").build();
    repo.save(dup);
    assertThatThrownBy(() -> em.flush())
        .isInstanceOf(PersistenceException.class);
  }

  @Test
  @DisplayName("JPA auditing populates createdAt/updatedAt and auditor names")
  void auditing_fields_populated() {
    var p = Permission.builder().code("role.read").name("Read Roles").build();
    var saved = repo.save(p);

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    // createdBy/lastModifiedBy depend on AuditorAware (AuditConfig). Fallback is "system".
    assertThat(saved.getCreatedBy()).isNotBlank();
    assertThat(saved.getLastModifiedBy()).isNotBlank();

    var originalUpdatedAt = saved.getUpdatedAt();

    // Update to trigger @LastModified* updates
    saved.setDescription("test update");
    var updated = repo.save(saved);

    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    assertThat(updated.getLastModifiedBy()).isNotBlank();
  }

  @Test
  @DisplayName("equals/hashCode are based on business key 'code'")
  void equals_hashCode_contract() {
    var p1 = Permission.builder().code("user.update").name("A").build();
    var p2 = Permission.builder().code("user.update").name("B").build();
    var p3 = Permission.builder().code("user.delete").name("C").build();

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    assertThat(p1).isNotEqualTo(p3);
  }

  @Test
  @DisplayName("createdAt timestamps are in the past or now (UTC semantics)")
  void timestamps_are_sane() {
    var before = Instant.now();
    var p = Permission.builder().code("client.read").name("Read Clients").build();
    var saved = repo.save(p);
    assertThat(saved.getCreatedAt()).isBetween(before.minusSeconds(5), Instant.now().plusSeconds(5));
  }
}