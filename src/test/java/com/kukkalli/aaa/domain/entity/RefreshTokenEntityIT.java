package com.kukkalli.aaa.domain.entity;

import com.kukkalli.aaa.domain.repository.RefreshTokenRepository;
import com.kukkalli.aaa.domain.repository.UserRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RefreshTokenEntityIT extends SpringBootITBase {

    @Autowired private RefreshTokenRepository repo;
    @Autowired private UserRepository userRepo;
    @Autowired private EntityManager em;

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

    @Test
    @DisplayName("CRUD round-trip with user FK persists and loads fields")
    void crud_roundtrip() {
        var u = newUser("rachel");
        var now = Instant.now();

        var rt = RefreshToken.builder()
                .user(u)
                .tokenHash("abc123hash")
                .issuedAt(now)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .ipAddress("203.0.113.1")
                .userAgent("JUnit")
                .build();

        var saved = repo.save(rt);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIssuedAt()).isNotNull(); // @CreatedDate should populate

        em.flush();
        em.clear();

        var found = repo.findById(saved.getId()).orElseThrow();
        assertThat(found.getUser().getUsername()).isEqualTo("rachel");
        assertThat(found.getTokenHash()).isEqualTo("abc123hash");
        assertThat(found.getExpiresAt()).isAfter(now);
        assertThat(found.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("isExpired() reflects expiresAt; revoke() flips flags and timestamp")
    void expiry_and_revoke_helpers() {
        var u = newUser("sam");
        var rt = repo.save(RefreshToken.builder()
                .user(u)
                .tokenHash("hash_exp")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().minusSeconds(5)) // already expired
                .revoked(false)
                .build());

        assertThat(rt.isExpired()).isTrue();

        rt.revoke();
        var updated = repo.save(rt);
        assertThat(updated.isRevoked()).isTrue();
        assertThat(updated.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("Unique constraint on tokenHash is enforced at DB level")
    void unique_token_hash() {
        var u = newUser("tony");
        repo.save(RefreshToken.builder()
                .user(u).tokenHash("dupHash").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).revoked(false).build());

        repo.save(RefreshToken.builder()
                .user(u).tokenHash("dupHash").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(120)).revoked(false).build());

        assertThatThrownBy(() -> em.flush()).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("Repository finders: by tokenHash, by user")
    void finders_by_hash_and_user() {
        var u1 = newUser("uma");
        var u2 = newUser("victor");

        var a = repo.save(RefreshToken.builder()
                .user(u1).tokenHash("h1").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build());
        var b = repo.save(RefreshToken.builder()
                .user(u1).tokenHash("h2").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build());
        var c = repo.save(RefreshToken.builder()
                .user(u2).tokenHash("h3").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build());

        assertThat(repo.findByTokenHash("h2")).isPresent().get().extracting(RefreshToken::getId).isEqualTo(b.getId());

        List<RefreshToken> u1Tokens = repo.findByUser(u1);
        assertThat(u1Tokens).extracting(RefreshToken::getTokenHash).containsExactlyInAnyOrder("h1","h2");

        List<RefreshToken> u2Tokens = repo.findByUser(u2);
        assertThat(u2Tokens).extracting(RefreshToken::getTokenHash).containsExactly("h3");
    }

    @Test
    @DisplayName("deleteByUser removes all tokens for a user; deleteByExpiresAtBefore prunes expired")
    void delete_helpers() {
        var u = newUser("will");
        var now = Instant.now();

        // expired
        repo.save(RefreshToken.builder()
                .user(u).tokenHash("old").issuedAt(now.minusSeconds(120)).expiresAt(now.minusSeconds(60)).build());
        // valid
        repo.save(RefreshToken.builder()
                .user(u).tokenHash("new").issuedAt(now).expiresAt(now.plusSeconds(3600)).build());

        long pruned = repo.deleteByExpiresAtBefore(now);
        assertThat(pruned).isEqualTo(1);

        long removed = repo.deleteByUser(u);
        assertThat(removed).isEqualTo(1);
    }

    @Test
    @DisplayName("findByRevokedFalseAndExpiresAtAfter returns only active, non-expired tokens")
    void find_active_tokens() {
        var u = newUser("xavier");
        var now = Instant.now();

        // active
        repo.save(RefreshToken.builder()
                .user(u).tokenHash("active").issuedAt(now).expiresAt(now.plusSeconds(600)).revoked(false).build());
        // revoked
        repo.save(RefreshToken.builder()
                .user(u).tokenHash("revoked").issuedAt(now).expiresAt(now.plusSeconds(600)).revoked(true).build());
        // expired
        repo.save(RefreshToken.builder()
                .user(u).tokenHash("expired").issuedAt(now).expiresAt(now.minusSeconds(1)).revoked(false).build());

        var active = repo.findByRevokedFalseAndExpiresAtAfter(now);
        assertThat(active).extracting(RefreshToken::getTokenHash).containsExactly("active");
    }
}