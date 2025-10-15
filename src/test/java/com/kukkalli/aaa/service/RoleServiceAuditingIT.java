package com.kukkalli.aaa.service;

import com.kukkalli.aaa.domain.entity.AuditLog;
import com.kukkalli.aaa.domain.repository.AuditLogRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import com.kukkalli.aaa.web.dto.CreateRoleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoleServiceAuditingIT extends SpringBootITBase {

    @Autowired private RoleService roleService;
    @Autowired private AuditLogRepository auditRepo;

    @Test
    void createRole_emitsAuditEvent() {
        var req = new CreateRoleRequest("ROLE_AUDIT_TEST", "Audit Test", "for tests", List.of("user.read"));
        var role = roleService.createRole(req);

        // There should be a ROLE_CREATE audit entry
        var last = auditRepo.findTop20ByOrderByCreatedAtDesc();
        assertThat(last.stream().map(AuditLog::getAction)).anyMatch("ROLE_CREATE"::equals);
        assertThat(role.getId()).isNotNull();
    }
}