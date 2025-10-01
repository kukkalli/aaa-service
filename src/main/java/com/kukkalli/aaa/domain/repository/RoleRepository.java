package com.kukkalli.aaa.domain.repository;

import com.kukkalli.aaa.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
