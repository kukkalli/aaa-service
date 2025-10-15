package com.kukkalli.aaa.domain.entity;

import com.kukkalli.aaa.domain.repository.PermissionRepository;
import com.kukkalli.aaa.domain.repository.RoleRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

class RoleEntityIT extends SpringBootITBase {

    @Autowired private RoleRepository roleRepo;
    @Autowired private PermissionRepository permRepo;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("CRUD round-trip persists and loads Role main fields")
    void crud_roundtrip() {
        var role = Role.builder()
                .code("ROLE_TESTER")
                .name("Tester")
                .description("Test role")
                .build();

        var saved = roleRepo.save(role);
        assertThat(saved.getId()).isNotNull();

        em.flush();
        em.clear();

        var found = roleRepo.findById(saved.getId()).orElseThrow();
        assertThat(found.getCode()).isEqualTo("ROLE_TESTER");
        assertThat(found.getName()).isEqualTo("Tester");
        assertThat(found.getDescription()).isEqualTo("Test role");
    }

    @Test
    @DisplayName("Unique constraint on role code is enforced")
    void unique_code_constraint() {
        roleRepo.save(Role.builder().code("ROLE_UNIQUE").name("Unique A").build());

        // Repo-level (case-insensitive) check
        assertThat(roleRepo.existsByCodeIgnoreCase("role_unique")).isTrue();

        // DB-level uniqueness on same exact code → flush should fail
        roleRepo.save(Role.builder().code("ROLE_UNIQUE").name("Unique B").build());
        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("Auditing populates createdAt/updatedAt and createdBy/lastModifiedBy")
    void auditing_fields_populated() {
        var saved = roleRepo.save(Role.builder().code("ROLE_AUDIT").name("Audit").build());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isNotBlank();       // typically "system" from AuditorAware fallback
        assertThat(saved.getLastModifiedBy()).isNotBlank();

        var prevUpdated = saved.getUpdatedAt();
        saved.setDescription("changed");
        var updated = roleRepo.save(saved);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(prevUpdated);
    }

    @Test
    @Transactional
    @DisplayName("Role ↔ Permission mapping works (add/remove, join table)")
    void permissions_mapping_add_remove() {
        // Given two permissions
        var pRead  = permRepo.save(Permission.builder().code("demo.read").name("Demo Read").build());
        var pWrite = permRepo.save(Permission.builder().code("demo.write").name("Demo Write").build());

        var role = roleRepo.save(Role.builder().code("ROLE_DEMO").name("Demo").build());

        // Add permissions via helpers
        role.addPermission(pRead);
        role.addPermission(pWrite);
        role = roleRepo.save(role);

        em.flush();
        em.clear();

        var found = roleRepo.findById(role.getId()).orElseThrow();
        assertThat(found.getPermissions()).extracting(Permission::getCode)
                .containsExactlyInAnyOrder("demo.read", "demo.write");

        // Remove one permission
        found.getPermissions().removeIf(p -> "demo.write".equals(p.getCode()));
        roleRepo.save(found);

        em.flush();
        em.clear();

        var afterRemove = roleRepo.findById(role.getId()).orElseThrow();
        assertThat(afterRemove.getPermissions()).extracting(Permission::getCode)
                .containsExactly("demo.read");
    }

    @Test
    @DisplayName("equals/hashCode are based on business key 'code'")
    void equals_hashcode_contract() {
        var a = Role.builder().code("ROLE_EQ").name("A").build();
        var b = Role.builder().code("ROLE_EQ").name("B").build();
        var c = Role.builder().code("ROLE_OTHER").name("C").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }
}