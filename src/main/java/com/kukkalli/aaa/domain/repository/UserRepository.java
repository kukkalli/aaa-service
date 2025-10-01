package com.kukkalli.aaa.domain.repository;

import com.kukkalli.aaa.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String usernamePart, String emailPart, Pageable pageable
    );

    /** Load user with roles (and permissions) for authorization checks. */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findOneWithRolesByUsernameIgnoreCase(String username);
}
