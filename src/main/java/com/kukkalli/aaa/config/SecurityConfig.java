package com.kukkalli.aaa.config;

import com.kukkalli.aaa.domain.entity.Permission;
import com.kukkalli.aaa.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // ---- CORS properties (mapped from application.yml -> cors.*) ----
    @Value("${cors.allowed-origins:*}")
    private String allowedOriginsCsv;

    @Value("${cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethodsCsv;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeadersCsv;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    private final com.kukkalli.aaa.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(com.kukkalli.aaa.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // ---- Password encoder --------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is a sensible default for REST APIs
        return new BCryptPasswordEncoder();
    }

    // ---- UserDetailsService backed by DB ------------------------------

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            var user = userRepository.findOneWithRolesByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // Map roles and permissions to authorities
            Set<SimpleGrantedAuthority> authorities =
                    user.getRoles().stream()
                            .flatMap(role -> {
                                var roleAuth = new SimpleGrantedAuthority(role.getCode()); // e.g., ROLE_ADMIN
                                var permAuths = role.getPermissions().stream()
                                        .map(Permission::getCode)                  // e.g., "user.read"
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toSet());
                                permAuths.add(roleAuth);
                                return permAuths.stream();
                            })
                            .collect(Collectors.toSet());

            // Use Spring Security's User (not your domain User)
            return User.builder()
                    .username(user.getUsername())
                    .password(user.getPasswordHash())
                    .disabled(!user.isEnabled())
                    .accountLocked(!user.isAccountNonLocked())
                    .accountExpired(!user.isAccountNonExpired())
                    .credentialsExpired(!user.isCredentialsNonExpired())
                    .authorities(authorities)
                    .build();
        };
    }

    // ---- Modern AuthenticationManager (no DaoAuthenticationProvider) --

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        var builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        return builder.build();
    }

    // ---- Security filter chain (JWT filter will be added later) -------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless REST API
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())

                // Exceptions: 401 for unauthenticated, 403 for access denied
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) -> res.sendError(org.springframework.http.HttpStatus.FORBIDDEN.value()))
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public actuator basics
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // OpenAPI / Swagger
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/openapi/**").permitAll()

                        // Auth endpoints public
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // OPTIONS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Everything else requires auth
                        .anyRequest().authenticated()
                );

        // JWT filter will be added here later, e.g.:
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ---- CORS configuration -------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(splitCsv(allowedOriginsCsv));
        cfg.setAllowedMethods(splitCsv(allowedMethodsCsv));
        cfg.setAllowedHeaders(splitCsv(allowedHeadersCsv));
        cfg.setAllowCredentials(allowCredentials);
        cfg.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
