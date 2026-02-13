package ua.kpi.sc.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized password-reset configuration bound to the {@code app.password-reset} prefix.
 *
 * @since 0.1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {
    private long tokenExpiration = 3600000; // 1 hour
    private String baseUrl = "http://localhost:3000";
}
