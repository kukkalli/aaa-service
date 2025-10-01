package com.kukkalli.aaa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AAA Service Application
 *
 * Provides Authentication, Authorization and Auditing APIs.
 * Runs with Spring Boot 3.5.6 and JDK 21.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware") // will wire from AuditConfig
@EnableAsync       // enables @Async methods (e.g. audit logging)
@EnableScheduling  // enables @Scheduled jobs (e.g. cleanup tasks)
public class AaaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AaaServiceApplication.class, args);
	}

}
