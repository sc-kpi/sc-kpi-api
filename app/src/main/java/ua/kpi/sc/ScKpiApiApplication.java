package ua.kpi.sc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the SC-KPI API modular monolith.
 *
 * <p>Scans the {@code ua.kpi.sc} base package to auto-detect components
 * across all modules (auth, user, engagements, council, document, notification, audit).
 *
 * @since 0.1.0
 */
@SpringBootApplication(scanBasePackages = "ua.kpi.sc")
public class ScKpiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScKpiApiApplication.class, args);
    }
}
