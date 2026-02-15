package ua.kpi.sc.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Contributes a {@code modules} key to the {@code /actuator/info} endpoint
 * listing every module in the SC-KPI modular monolith.
 *
 * @since 0.1.0
 */
@Component
public class ModuleInfoContributor implements InfoContributor {

    private static final List<Map<String, String>> MODULES = List.of(
            module("common", "ua.kpi.sc.common",
                    "Shared infrastructure: exceptions, security, utilities"),
            module("auth", "ua.kpi.sc.auth",
                    "Authentication and authorization (JWT, security filter chain)"),
            module("user", "ua.kpi.sc.user",
                    "User profiles and account management"),
            module("engagements", "ua.kpi.sc.engagements",
                    "External engagement tracking and partnerships"),
            module("council", "ua.kpi.sc.council",
                    "Internal council operations and workflows"),
            module("document", "ua.kpi.sc.document",
                    "Document generation and template management"),
            module("notification", "ua.kpi.sc.notification",
                    "Notification delivery (email, push, in-app)"),
            module("audit", "ua.kpi.sc.audit",
                    "Audit logging and change tracking"),
            module("feature-flag", "ua.kpi.sc.featureflag",
                    "Feature flag management and evaluation")
    );

    private static Map<String, String> module(String name, String pkg, String description) {
        return Map.of("name", name, "package", pkg, "description", description);
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("modules", Map.of(
                "count", MODULES.size(),
                "list", MODULES
        ));
    }
}
