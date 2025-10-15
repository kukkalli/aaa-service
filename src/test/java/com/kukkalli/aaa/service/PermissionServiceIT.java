package com.kukkalli.aaa.service;

import com.kukkalli.aaa.domain.entity.AuditLog;
import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.repository.AuditLogRepository;
import com.kukkalli.aaa.domain.repository.PermissionRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import com.kukkalli.aaa.web.dto.CreatePermissionRequest;
import com.kukkalli.aaa.web.dto.UpdatePermissionRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PermissionServiceIT extends SpringBootITBase {

    @Autowired private PermissionService service;
    @Autowired private PermissionRepository permRepo;
    @Autowired private AuditLogRepository auditRepo;
    @Autowired private EntityManager em;

    // ------------------------- helpers -------------------------

    private static Condition<AuditLog> hasAction(String action) {
        return new Condition<>(al -> action.equals(al.getAction()),
                "action = %s", action);
    }

    private static Condition<AuditLog> hasDetail(String key, String expectedVal) {
        return new Condition<>(al -> {
            Map<String, Object> d = al.getDetails();
            return d != null && expectedVal.equals(String.valueOf(d.get(key)));
        }, "details[%s] = %s", key, expectedVal);
    }

    // -------------------------- tests --------------------------

    @Test
    @DisplayName("create(): persists permission and emits PERMISSION_CREATE audit with targetType/targetId via SpEL")
    void create_emits_audit() {
        var before = Instant.now();

        var req = new CreatePermissionRequest("perm.create.it", "Create IT", "created by test");
        Permission created = service.create(req);
        assertThat(created.getId()).isNotNull();

        // verify persisted
        assertThat(permRepo.findByCodeIgnoreCase("perm.create.it")).isPresent();

        // verify audit (action + SpEL targetId = req.code, targetType = PERMISSION)
        var page = auditRepo.findByAction("PERMISSION_CREATE", PageRequest.of(0, 20));
        assertThat(page.getContent()).isNotEmpty();

        // choose recent entries only
        var recent = page.getContent().stream()
                .filter(a -> !a.getOccurredAt().isBefore(before))
                .toList();

        assertThat(recent).isNotEmpty();
        assertThat(recent).anySatisfy(al -> {
            assertThat(al).has(hasAction("PERMISSION_CREATE"));
            Map<String, Object> d = al.getDetails();
            assertThat(d).isNotNull();
            assertThat(d.get("targetType")).isEqualTo("PERMISSION");
            assertThat(String.valueOf(d.get("targetId"))).isEqualTo("perm.create.it");
            assertThat(String.valueOf(d.get("method"))).contains("PermissionService.create");
        });
    }

    @Test
    @DisplayName("update(): changes persisted fields and emits PERMISSION_UPDATE audit with targetId = id")
    void update_emits_audit() {
        var created = permRepo.save(Permission.builder()
                .code("perm.update.it")
                .name("Old Name")
                .description("old")
                .build());

        var before = Instant.now();

        var req = new UpdatePermissionRequest("New Name", "new desc");
        Permission updated = service.update(created.getId(), req);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDescription()).isEqualTo("new desc");

        var page = auditRepo.findByAction("PERMISSION_UPDATE", PageRequest.of(0, 20));
        var recent = page.getContent().stream()
                .filter(a -> !a.getOccurredAt().isBefore(before))
                .toList();

        assertThat(recent).isNotEmpty();
        assertThat(recent).anySatisfy(al -> {
            assertThat(al).has(hasAction("PERMISSION_UPDATE"));
            Map<String, Object> d = al.getDetails();
            assertThat(d).isNotNull();
            // targetId comes from @Audited(targetId = "#id") → String value of id
            assertThat(String.valueOf(d.get("targetId"))).isEqualTo(String.valueOf(created.getId()));
            assertThat(String.valueOf(d.get("targetType"))).isEqualTo("PERMISSION");
            assertThat(String.valueOf(d.get("method"))).contains("PermissionService.update");
        });
    }

    @Test
    @DisplayName("delete(): removes entity and emits PERMISSION_DELETE audit with targetId = id")
    void delete_emits_audit() {
        var p = permRepo.save(Permission.builder()
                .code("perm.delete.it")
                .name("To Delete")
                .build());

        var id = p.getId();
        var before = Instant.now();

        service.delete(id);
        em.flush();

        assertThat(permRepo.findById(id)).isEmpty();

        var page = auditRepo.findByAction("PERMISSION_DELETE", PageRequest.of(0, 20));
        var recent = page.getContent().stream()
                .filter(a -> !a.getOccurredAt().isBefore(before))
                .toList();

        assertThat(recent).isNotEmpty();
        assertThat(recent).anySatisfy(al -> {
            Map<String, Object> d = al.getDetails();
            assertThat(d).isNotNull();
            assertThat(String.valueOf(d.get("targetId"))).isEqualTo(String.valueOf(id));
            assertThat(String.valueOf(d.get("targetType"))).isEqualTo("PERMISSION");
        });
    }

    @Test
    @DisplayName("getByCodeOrThrow(): returns existing or throws EntityNotFoundException")
    void getByCodeOrThrow_behaviour() {
        permRepo.save(Permission.builder().code("perm.get.it").name("Get It").build());

        var found = service.getByCodeOrThrow("perm.get.it");
        assertThat(found.getName()).isEqualTo("Get It");

        assertThatThrownBy(() -> service.getByCodeOrThrow("does.not.exist"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Permission not found");
    }

    @Test
    @DisplayName("list/finders: pageable list + findById/findByCode work")
    void list_and_finders() {
        var a = permRepo.save(Permission.builder().code("perm.a").name("A").build());
        var b = permRepo.save(Permission.builder().code("perm.b").name("B").build());
        var c = permRepo.save(Permission.builder().code("perm.c").name("C").build());

        var page1 = service.list(PageRequest.of(0, 2));
        assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page1.getContent().size()).isEqualTo(2);

        assertThat(service.findById(a.getId())).isPresent();
        assertThat(service.findByCode("perm.c")).isPresent();
    }
}