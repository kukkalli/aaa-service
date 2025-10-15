package com.kukkalli.aaa.domain.entity;

import com.kukkalli.aaa.domain.repository.ApiClientRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

class ApiClientEntityIT extends SpringBootITBase {

    @Autowired private ApiClientRepository repo;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("CRUD round-trip persists and loads all core fields")
    void crud_roundtrip() {
        var ac = ApiClient.builder()
                .clientId("svc_demo")
                .clientSecretHash("$2a$10$deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbe")
                .name("Demo Service")
                .scopes("user.read,client.read")
                .allowedIps("10.0.0.0/24,203.0.113.10")
                .enabled(true)
                .build();

        var saved = repo.save(ac);
        assertThat(saved.getId()).isNotNull();

        em.flush();
        em.clear();

        var found = repo.findById(saved.getId()).orElseThrow();
        assertThat(found.getClientId()).isEqualTo("svc_demo");
        assertThat(found.getName()).isEqualTo("Demo Service");
        assertThat(found.getScopes()).isEqualTo("user.read,client.read");
        assertThat(found.getAllowedIps()).isEqualTo("10.0.0.0/24,203.0.113.10");
        assertThat(found.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Unique constraint on clientId is enforced (repo + DB)")
    void unique_clientId() {
        repo.save(ApiClient.builder()
                .clientId("svc_unique")
                .clientSecretHash("x")
                .name("A")
                .build());

        // repo helpers
        assertThat(repo.existsByClientIdIgnoreCase("SVC_UNIQUE")).isTrue();
        assertThat(repo.findByClientIdIgnoreCase("svc_unique")).isPresent();

        // DB-level uniqueness on same ID → flush should fail
        repo.save(ApiClient.builder()
                .clientId("svc_unique")
                .clientSecretHash("y")
                .name("B")
                .build());

        assertThatThrownBy(() -> em.flush()).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("findByClientIdAndEnabledTrue returns only enabled clients")
    void find_enabled_true() {
        repo.save(ApiClient.builder().clientId("svc_enabled").clientSecretHash("x").name("Enabled").enabled(true).build());
        repo.save(ApiClient.builder().clientId("svc_disabled").clientSecretHash("x").name("Disabled").enabled(false).build());

        assertThat(repo.findByClientIdAndEnabledTrue("svc_enabled")).isPresent();
        assertThat(repo.findByClientIdAndEnabledTrue("svc_disabled")).isEmpty();
    }

    @Test
    @DisplayName("Auditing populates createdAt/updatedAt and createdBy/lastModifiedBy")
    void auditing_fields() {
        var saved = repo.save(ApiClient.builder()
                .clientId("svc_audit")
                .clientSecretHash("x")
                .name("Audit")
                .build());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isNotBlank();       // typically "system"
        assertThat(saved.getLastModifiedBy()).isNotBlank();

        var prevUpdated = saved.getUpdatedAt();
        saved.setName("Audit Renamed");
        var updated = repo.save(saved);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(prevUpdated);
    }

    @Test
    @DisplayName("equals/hashCode are based on business key 'clientId'")
    void equals_hashcode_contract() {
        var a = ApiClient.builder().clientId("svc_eq").clientSecretHash("x").name("A").build();
        var b = ApiClient.builder().clientId("svc_eq").clientSecretHash("y").name("B").build();
        var c = ApiClient.builder().clientId("svc_other").clientSecretHash("z").name("C").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }
}