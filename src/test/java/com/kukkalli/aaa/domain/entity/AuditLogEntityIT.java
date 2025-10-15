package com.kukkalli.aaa.domain.entity;

import com.kukkalli.aaa.domain.repository.ApiClientRepository;
import com.kukkalli.aaa.domain.repository.AuditLogRepository;
import com.kukkalli.aaa.domain.repository.UserRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogEntityIT extends SpringBootITBase {

    @Autowired private AuditLogRepository auditRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ApiClientRepository clientRepo;

    private User newUser(String username) {
        return userRepo.save(User.builder()
                .username(username)
                .email(username + "@example.com")
                .passwordHash("x")
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .build());
    }

    private ApiClient newClient(String id, boolean enabled) {
        return clientRepo.save(ApiClient.builder()
                .clientId(id)
                .clientSecretHash("hash")
                .name("Client " + id)
                .enabled(enabled)
                .build());
    }

    @Test
    @DisplayName("Persist AuditLog with actor user, JSON details, and timestamps")
    void persist_with_user_and_json_details() {
        var u = newUser("auditor1");

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", "UserService.create");
        details.put("targetType", "USER");
        details.put("targetId", "42");
        details.put("flags", List.of("x","y"));

        var when = Instant.now().minusSeconds(5);

        var log = AuditLog.builder()
                .occurredAt(when)
                .actorUser(u)
                .action("USER_CREATE")
                .targetType("USER")
                .targetId("42")
                .requestId("req-1")
                .ipAddress("203.0.113.9")
                .userAgent("JUnit")
                .details(details)
                .build();

        var saved = auditRepo.save(log);
        assertThat(saved.getId()).isNotNull();

        // The DB default should set created_at; occurred_at is our explicit value
        assertThat(saved.getOccurredAt()).isEqualTo(when);
        assertThat(saved.getCreatedAt()).isNotNull();

        var roundTrip = auditRepo.findById(saved.getId()).orElseThrow();
        assertThat(roundTrip.getActorUser().getUsername()).isEqualTo("auditor1");
        assertThat(roundTrip.getAction()).isEqualTo("USER_CREATE");
        assertThat(roundTrip.getDetails()).isNotNull();
        assertThat(roundTrip.getDetails()).containsEntry("targetId", "42");
        assertThat(roundTrip.getDetails().toString()).contains("x", "y");
    }

    @Test
    @DisplayName("Persist AuditLog with actor client; finder by client works")
    void persist_with_client_and_query() {
        var client = newClient("svc_audit", true);

        var l1 = auditRepo.save(AuditLog.builder()
                .occurredAt(Instant.now().minusSeconds(30))
                .actorClient(client)
                .action("CLIENT_CALL")
                .requestId("c-1")
                .details(Map.of("endpoint", "/api/v1/users"))
                .build());

        var l2 = auditRepo.save(AuditLog.builder()
                .occurredAt(Instant.now().minusSeconds(20))
                .actorClient(client)
                .action("CLIENT_CALL")
                .requestId("c-2")
                .details(Map.of("endpoint", "/api/v1/roles"))
                .build());

        var page = auditRepo.findByActorClient(client, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).extracting(AuditLog::getRequestId)
                .contains("c-1", "c-2");
    }

    @Test
    @DisplayName("Finder by action and by occurredAt range return expected entries")
    void find_by_action_and_time_range() {
        var now = Instant.now();

        var a1 = auditRepo.save(AuditLog.builder()
                .occurredAt(now.minusSeconds(60))
                .action("AUTH_LOGIN")
                .requestId("t-1")
                .build());

        var a2 = auditRepo.save(AuditLog.builder()
                .occurredAt(now.minusSeconds(10))
                .action("AUTH_LOGIN")
                .requestId("t-2")
                .build());

        var a3 = auditRepo.save(AuditLog.builder()
                .occurredAt(now.minusSeconds(5))
                .action("USER_UPDATE")
                .requestId("t-3")
                .build());

        // by action
        var byAction = auditRepo.findByAction("AUTH_LOGIN", PageRequest.of(0, 10));
        assertThat(byAction.getContent()).extracting(AuditLog::getRequestId)
                .contains("t-1","t-2")
                .doesNotContain("t-3");

        // by time range: only last ~15s
        var byTime = auditRepo.findByOccurredAtBetween(now.minusSeconds(15),
                now.plusSeconds(5), PageRequest.of(0, 10));
        assertThat(byTime.getContent()).extracting(AuditLog::getRequestId)
                .contains("t-2","t-3")
                .doesNotContain("t-1");
    }

    @Test
    @DisplayName("TopN finders order by createdAt desc")
    void topN_createdAt_desc() throws InterruptedException {
        // create a few rows
        auditRepo.save(AuditLog.builder().occurredAt(Instant.now().minusSeconds(3)).
                action("A").requestId("r1").build());
        Thread.sleep(5); // ensure created_at ordering difference
        auditRepo.save(AuditLog.builder().occurredAt(Instant.now().minusSeconds(2)).
                action("B").requestId("r2").build());
        Thread.sleep(5);
        auditRepo.save(AuditLog.builder().occurredAt(Instant.now().minusSeconds(1)).
                action("C").requestId("r3").build());

        var top10 = auditRepo.findTop10ByOrderByCreatedAtDesc();
        assertThat(top10).isNotEmpty();
        // first element should be the most recently created
        var createdAtFirst = top10.getFirst().getCreatedAt();
        // assert strictly non-increasing order
        for (int i = 1; i < Math.min(3, top10.size()); i++) {
            assertThat(top10.get(i).getCreatedAt()).isBeforeOrEqualTo(createdAtFirst);
            createdAtFirst = top10.get(i).getCreatedAt();
        }
    }

    @Test
    @DisplayName("Finder by actor user returns only that user’s entries")
    void find_by_actor_user() {
        var u1 = newUser("ua");
        var u2 = newUser("ub");

        var e1 = auditRepo.save(AuditLog.builder().occurredAt(Instant.now()).actorUser(u1).action("E").
                requestId("u1-1").build());
        var e2 = auditRepo.save(AuditLog.builder().occurredAt(Instant.now()).actorUser(u1).action("E").
                requestId("u1-2").build());
        var e3 = auditRepo.save(AuditLog.builder().occurredAt(Instant.now()).actorUser(u2).action("E").
                requestId("u2-1").build());

        var pageU1 = auditRepo.findByActorUser(u1, PageRequest.of(0,10));
        assertThat(pageU1.getContent()).extracting(AuditLog::getRequestId).contains("u1-1","u1-2").
                doesNotContain("u2-1");

        var pageU2 = auditRepo.findByActorUser(u2, PageRequest.of(0,10));
        assertThat(pageU2.getContent()).extracting(AuditLog::getRequestId).contains("u2-1").
                doesNotContain("u1-1","u1-2");
    }
}