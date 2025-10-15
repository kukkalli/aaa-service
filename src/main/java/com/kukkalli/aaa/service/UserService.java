package com.kukkalli.aaa.service;

import com.kukkalli.aaa.domain.entity.Role;
import com.kukkalli.aaa.domain.entity.User;
import com.kukkalli.aaa.domain.repository.RoleRepository;
import com.kukkalli.aaa.domain.repository.UserRepository;
import com.kukkalli.aaa.web.dto.CreateUserRequest;
import com.kukkalli.aaa.web.dto.UpdateUserRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepo,
                       RoleRepository roleRepo,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepo = Objects.requireNonNull(userRepo);
        this.roleRepo = Objects.requireNonNull(roleRepo);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.auditService = Objects.requireNonNull(auditService);
    }

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<User> search(String q, Pageable pageable) {
        if (StringUtils.hasText(q)) {
            return userRepo.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, pageable);
        }
        return userRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepo.findById(id);
    }

    // ---------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------

    @Transactional
    public User create(CreateUserRequest req) {
        // Uniqueness checks
        if (userRepo.existsByUsernameIgnoreCase(req.username())) {
            throw new IllegalArgumentException("Username already exists: " + req.username());
        }
        if (userRepo.existsByEmailIgnoreCase(req.email())) {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        }

        var user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .enabled(true)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .phone(req.phone())
                .roles(new LinkedHashSet<>())
                .build();

        // Assign roles if provided
        if (!CollectionUtils.isEmpty(req.roles())) {
            for (String code : req.roles()) {
                Role role = roleRepo.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + code));
                user.addRole(role);
            }
        }

        User saved = userRepo.save(user);

        // Audit
        auditService.audit("USER_CREATE", Map.of("id", saved.getId(), "username", saved.getUsername()));

        return saved;
    }

    @Transactional
    public User update(Long id, UpdateUserRequest req) {
        var user = userRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        boolean changed = false;

        if (StringUtils.hasText(req.email()) && !Objects.equals(req.email(), user.getEmail())) {
            // Optional: enforce email uniqueness here if desired
            if (userRepo.existsByEmailIgnoreCase(req.email())) {
                throw new IllegalArgumentException("Email already exists: " + req.email());
            }
            user.setEmail(req.email());
            changed = true;
        }
        if (StringUtils.hasText(req.firstName()) && !Objects.equals(req.firstName(), user.getFirstName())) {
            user.setFirstName(req.firstName());
            changed = true;
        }
        if (StringUtils.hasText(req.lastName()) && !Objects.equals(req.lastName(), user.getLastName())) {
            user.setLastName(req.lastName());
            changed = true;
        }
        if (StringUtils.hasText(req.phone()) && !Objects.equals(req.phone(), user.getPhone())) {
            user.setPhone(req.phone());
            changed = true;
        }

        if (req.enabled() != null && user.isEnabled() != req.enabled()) {
            user.setEnabled(req.enabled());
            changed = true;
        }
        if (req.accountNonLocked() != null && user.isAccountNonLocked() != req.accountNonLocked()) {
            user.setAccountNonLocked(req.accountNonLocked());
            changed = true;
        }
        if (req.accountNonExpired() != null && user.isAccountNonExpired() != req.accountNonExpired()) {
            user.setAccountNonExpired(req.accountNonExpired());
            changed = true;
        }
        if (req.credentialsNonExpired() != null && user.isCredentialsNonExpired() != req.credentialsNonExpired()) {
            user.setCredentialsNonExpired(req.credentialsNonExpired());
            changed = true;
        }

        if (req.roles() != null) {
            // Replace roles with a provided list
            user.getRoles().clear();
            for (String code : req.roles()) {
                Role role = roleRepo.findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + code));
                user.addRole(role);
            }
            changed = true;
        }

        if (changed) {
            user = userRepo.save(user);
            auditService.audit("USER_UPDATE", Map.of("id", id));
        }

        return user;
    }

    @Transactional
    public void disable(Long id) {
        var user = userRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        if (user.isEnabled()) {
            user.setEnabled(false);
            userRepo.save(user);
            auditService.audit("USER_DISABLE", Map.of("id", id));
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepo.existsById(id)) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        userRepo.deleteById(id);
        auditService.audit("USER_DELETE", Map.of("id", id));
    }

    @Transactional
    public User assignRoles(Long id, List<String> roleCodes) {
        var user = userRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        for (String code : roleCodes) {
            Role role = roleRepo.findByCodeIgnoreCase(code)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + code));
            user.addRole(role);
        }
        User saved = userRepo.save(user);
        auditService.audit("USER_ROLE_ASSIGN", Map.of("id", id, "roles", roleCodes));
        return saved;
    }

    @Transactional
    public User removeRole(Long id, String roleCode) {
        var user = userRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        var role = roleRepo.findByCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleCode));
        user.removeRole(role);
        User saved = userRepo.save(user);
        auditService.audit("USER_ROLE_REMOVE", Map.of("id", id, "role", roleCode));
        return saved;
    }
}
