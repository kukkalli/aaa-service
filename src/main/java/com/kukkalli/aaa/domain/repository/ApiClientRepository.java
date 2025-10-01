package com.kukkalli.aaa.domain.repository;

import com.kukkalli.aaa.domain.entity.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiClientRepository
        extends JpaRepository<ApiClient, Long>, JpaSpecificationExecutor<ApiClient> {

    Optional<ApiClient> findByClientIdIgnoreCase(String clientId);

    boolean existsByClientIdIgnoreCase(String clientId);

    Optional<ApiClient> findByClientIdAndEnabledTrue(String clientId);
}
