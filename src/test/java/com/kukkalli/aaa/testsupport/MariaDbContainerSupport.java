package com.kukkalli.aaa.testsupport;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class MariaDbContainerSupport {

    // One container for the whole test JVM
    static final MariaDBContainer<?> DB = new MariaDBContainer<>(
            DockerImageName.parse("mariadb:11.8.3")
    ).withDatabaseName("aaa")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startContainer() {
        if (!DB.isRunning()) DB.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // rely on Flyway
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
        r.add("aaa.seed.enabled", () -> "true"); // exercise DataSeeder
    }
}