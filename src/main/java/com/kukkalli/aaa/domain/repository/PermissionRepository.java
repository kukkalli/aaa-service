package com.kukkalli.aaa.domain.repository;

import com.kukkalli.aaa.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
