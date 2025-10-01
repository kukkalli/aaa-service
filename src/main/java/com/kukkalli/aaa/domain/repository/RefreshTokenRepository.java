package com.kukkalli.aaa.domain.repository;

import com.kukkalli.aaa.domain.entity.RefreshToken;
import com.kukkalli.aaa.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser(User user);

    long deleteByUser(User user);

    long deleteByExpiresAtBefore(Instant instant);

    List<RefreshToken> findByRevokedFalseAndExpiresAtAfter(Instant now);
}
