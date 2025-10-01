package com.kukkalli.aaa.bootstrap;

import com.kukkalli.aaa.domain.entity.ApiClient;
import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.entity.Role;
import com.kukkalli.aaa.domain.entity.User;
import com.kukkalli.aaa.domain.repository.ApiClientRepository;
import com.kukkalli.aaa.domain.repository.PermissionRepository;
import com.kukkalli.aaa.domain.repository.RoleRepository;
import com.kukkalli.aaa.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "aaa.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final PermissionRepository permRepo;
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final ApiClientRepository clientRepo;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    // Admin bootstrap (override in application-local.yml or env)
    @Value("${aaa.seed.admin.username:admin}")
    private String adminUsername;

    @Value("${aaa.seed.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${aaa.seed.admin.password:P@ssw0rd!}")
    private String adminPassword;

    // Optional API client bootstrap
    @Value("${aaa.seed.client.enabled:true}")
    private boolean seedClient;

    @Value("${aaa.seed.client.id:svc_bootstrap}")
    private String seedClientId;

    @Value("${aaa.seed.client.name:Bootstrap Client}")
    private String seedClientName;

    public DataSeeder(PermissionRepository permRepo,
                      RoleRepository roleRepo,
                      UserRepository userRepo,
                      ApiClientRepository clientRepo,
                      PasswordEncoder passwordEncoder) {
        this.permRepo = Objects.requireNonNull(permRepo);
        this.roleRepo = Objects.requireNonNull(roleRepo);
        this.userRepo = Objects.requireNonNull(userRepo);
        this.clientRepo = Objects.requireNonNull(clientRepo);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("AAA DataSeeder: seeding baseline data…");

        // 1) Seed permission catalog
        var permissions = List.of(
                // user
                "user.read", "user.create", "user.update", "user.delete",
                // role
                "role.read", "role.create", "role.update", "role.delete",
                // permission
                "permission.read", "permission.create", "permission.update", "permission.delete",
                // client
                "client.read", "client.create", "client.update", "client.delete",
                // audit
                "audit.read"
        );
        Map<String, Permission> permMap = ensurePermissions(permissions);

        // 2) Seed roles
        Role adminRole = ensureRole("ROLE_ADMIN", "Administrator",
                "Full access", new ArrayList<>(permMap.values()));

        Role userRole = ensureRole("ROLE_USER", "User",
                "Basic read access", List.of(permMap.get("user.read")));

        // 3) Seed admin user
        ensureAdminUser(adminUsername, adminEmail, adminPassword, adminRole);

        // 4) Optional: seed API client (one-time secret only printed when created)
        if (seedClient) {
            ensureApiClient(seedClientId, seedClientName, List.of("user.read", "client.read"));
        }

        log.info("AAA DataSeeder: done.");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Map<String, Permission> ensurePermissions(List<String> codes) {
        Map<String, Permission> out = new LinkedHashMap<>();
        for (String code : codes) {
            var p = permRepo.findByCodeIgnoreCase(code).orElseGet(() -> {
                var created = Permission.builder()
                        .code(code)
                        .name(humanize(code))
                        .description("Seeded permission: " + code)
                        .build();
                return permRepo.save(created);
            });
            out.put(code, p);
        }
        return out;
    }

    private Role ensureRole(String code, String name, String description, Collection<Permission> permissions) {
        return roleRepo.findByCodeIgnoreCase(code).orElseGet(() -> {
            Role r = Role.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .permissions(new LinkedHashSet<>(permissions))
                    .build();
            return roleRepo.save(r);
        });
    }

    private void ensureAdminUser(String username, String email, String rawPassword, Role adminRole) {
        userRepo.findByUsernameIgnoreCase(username)
                .or(() -> userRepo.findByEmailIgnoreCase(email))
                .ifPresentOrElse(
                        u -> {
                            // ensure it has admin role
                            if (u.getRoles().stream().noneMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getCode()))) {
                                u.addRole(adminRole);
                                userRepo.save(u);
                                log.info("Admin user existed; added ROLE_ADMIN: {}", u.getUsername());
                            } else {
                                log.info("Admin user already exists: {}", u.getUsername());
                            }
                        },
                        () -> {
                            User admin = User.builder()
                                    .username(username)
                                    .email(email)
                                    .passwordHash(passwordEncoder.encode(rawPassword))
                                    .enabled(true)
                                    .accountNonLocked(true)
                                    .accountNonExpired(true)
                                    .credentialsNonExpired(true)
                                    .roles(new LinkedHashSet<>(List.of(adminRole)))
                                    .build();
                            userRepo.save(admin);
                            log.warn("Created ADMIN user '{}'. Store credentials securely. (username='{}', password='{}')",
                                    admin.getUsername(), username, rawPassword);
                        }
                );
    }

    private void ensureApiClient(String clientId, String name, List<String> scopeCodes) {
        clientRepo.findByClientIdIgnoreCase(clientId).ifPresentOrElse(
                c -> log.info("API client already exists: {}", c.getClientId()),
                () -> {
                    String rawSecret = generateClientSecret();
                    var entity = ApiClient.builder()
                            .clientId(clientId)
                            .clientSecretHash(passwordEncoder.encode(rawSecret))
                            .name(name)
                            .scopes(String.join(",", scopeCodes))
                            .allowedIps(null)
                            .enabled(true)
                            .build();
                    var saved = clientRepo.save(entity);
                    log.warn("Created API client '{}'. SECRET IS SHOWN ONCE — STORE SECURELY NOW. clientId='{}', clientSecret='{}'",
                            saved.getName(), saved.getClientId(), rawSecret);
                }
        );
    }

    private String generateClientSecret() {
        byte[] buf = new byte[32]; // 256 bits
        random.nextBytes(buf);
        return "cs_" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String humanize(String code) {
        // "user.read" -> "User Read"
        return Arrays.stream(code.split("[._-]"))
                .map(s -> s.isBlank() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
