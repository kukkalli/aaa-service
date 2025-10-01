package com.kukkalli.aaa.domain.repository;

import com.kukkalli.aaa.domain.entity.AuditLog;
import com.kukkalli.aaa.domain.entity.ApiClient;
import com.kukkalli.aaa.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorUser(User user, Pageable pageable);

    Page<AuditLog> findByActorClient(ApiClient client, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByOccurredAtBetween(Instant from, Instant to, Pageable pageable);
}
