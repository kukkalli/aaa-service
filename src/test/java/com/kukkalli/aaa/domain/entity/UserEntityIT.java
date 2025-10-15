package com.kukkalli.aaa.domain.entity;

import com.kukkalli.aaa.domain.repository.RoleRepository;
import com.kukkalli.aaa.domain.repository.UserRepository;
import com.kukkalli.aaa.testsupport.SpringBootITBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

class UserEntityIT extends SpringBootITBase {

    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("CRUD round-trip persists and loads core fields")
    void crud_roundtrip() {
        var u = User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("{bcrypt}$2a$10$012345678901234567890u") // dummy hash
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .firstName("Alice")
                .lastName("Wonderland")
                .phone("123456789")
                .build();

        var saved = userRepo.save(u);
        assertThat(saved.getId()).isNotNull();

        em.flush();
        em.clear();

        var found = userRepo.findById(saved.getId()).orElseThrow();
        assertThat(found.getUsername()).isEqualTo("alice");
        assertThat(found.getEmail()).isEqualTo("alice@example.com");
        assertThat(found.getFirstName()).isEqualTo("Alice");
        assertThat(found.getLastName()).isEqualTo("Wonderland");
        assertThat(found.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Unique constraints on username and email are enforced")
    void unique_username_email() {
        userRepo.save(User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("x").build());

        // repo helpers
        assertThat(userRepo.existsByUsernameIgnoreCase("BOB")).isTrue();
        assertThat(userRepo.existsByEmailIgnoreCase("bob@example.com")).isTrue();

        // DB-level duplicate username → flush should fail
        userRepo.save(User.builder()
                .username("bob")
                .email("bob2@example.com")
                .passwordHash("y").build());

        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(PersistenceException.class);

        em.clear();

        // DB-level duplicate email → flush should fail
        userRepo.save(User.builder()
                .username("bobby")
                .email("bob@example.com")
                .passwordHash("z").build());

        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @Transactional
    @DisplayName("User ↔ Role mapping works (assign/remove roles)")
    void roles_mapping_add_remove() {
        var rUser  = roleRepo.save(Role.builder().code("ROLE_USERX").name("User X").build());
        var rAdmin = roleRepo.save(Role.builder().code("ROLE_ADMINX").name("Admin X").build());

        var u = userRepo.save(User.builder()
                .username("carol")
                .email("carol@example.com")
                .passwordHash("x")
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .build());

        // assign roles (helper)
        u.addRole(rUser);
        u.addRole(rAdmin);
        u = userRepo.save(u);

        em.flush();
        em.clear();

        var found = userRepo.findById(u.getId()).orElseThrow();
        assertThat(found.getRoles()).extracting(Role::getCode)
                .containsExactlyInAnyOrder("ROLE_USERX", "ROLE_ADMINX");

        // remove one
        found.removeRole(found.getRoles().stream()
                .filter(r -> r.getCode().equals("ROLE_USERX"))
                .findFirst().orElseThrow());
        userRepo.save(found);

        em.flush();
        em.clear();

        var after = userRepo.findById(u.getId()).orElseThrow();
        assertThat(after.getRoles()).extracting(Role::getCode)
                .containsExactly("ROLE_ADMINX");
    }

    @Test
    @DisplayName("Auditing populates createdAt/updatedAt and createdBy/lastModifiedBy")
    void auditing_fields() {
        var u = userRepo.save(User.builder()
                .username("dave")
                .email("dave@example.com")
                .passwordHash("x")
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .firstName("Dave")
                .build());

        assertThat(u.getCreatedAt()).isNotNull();
        assertThat(u.getUpdatedAt()).isNotNull();
        assertThat(u.getCreatedBy()).isNotBlank();       // typically "system"
        assertThat(u.getLastModifiedBy()).isNotBlank();

        var prevUpdated = u.getUpdatedAt();
        u.setFirstName("David");
        var updated = userRepo.save(u);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(prevUpdated);
    }

    @Test
    @DisplayName("equals/hashCode are based on business key 'username'")
    void equals_hashcode_contract() {
        var a = User.builder().username("erin").email("erin1@example.com").passwordHash("x").build();
        var b = User.builder().username("erin").email("erin2@example.com").passwordHash("y").build();
        var c = User.builder().username("eric").email("eric@example.com").passwordHash("z").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("findOneWithRolesByUsernameIgnoreCase loads roles (+ permissions)")
    void find_with_roles_entitygraph() {
        var p = Permission.builder().code("sample.read").name("Sample Read").build();
        var r = Role.builder().code("ROLE_SAMPLE").name("Sample").build();
        r.getPermissions().add(p); // establish role→permission

        var userRole = roleRepo.save(r); // cascades not set, so save order explicit

        var u = userRepo.save(User.builder()
                .username("frank")
                .email("frank@example.com")
                .passwordHash("x").build());
        u.addRole(userRole);
        userRepo.save(u);

        em.flush();
        em.clear();

        var loaded = userRepo.findOneWithRolesByUsernameIgnoreCase("FRANK").orElseThrow();
        assertThat(loaded.getRoles()).extracting(Role::getCode)
                .containsExactly("ROLE_SAMPLE");

        // Ensure role permissions were fetched through the EntityGraph path
        assertThat(loaded.getRoles().iterator().next().getPermissions())
                .extracting(Permission::getCode)
                .containsExactly("sample.read");
    }
}