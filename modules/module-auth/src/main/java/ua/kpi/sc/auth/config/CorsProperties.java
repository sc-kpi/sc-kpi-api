package ua.kpi.sc.auth.config;

import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized CORS configuration bound to the {@code cors.*} property prefix.
 *
 * <p>Defaults are suitable for local development (origin {@code http://localhost:3000}).
 * Override in production via environment variables or application properties:
 * <pre>
 * cors.allowed-origins=https://sc.kpi.ua
 * cors.allow-credentials=true
 * </pre>
 *
 * @see SecurityFilterChainConfig#corsConfigurationSource()
 * @since 0.1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private List<String> allowedOrigins = List.of("http://localhost:3000");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private boolean allowCredentials = true;
    private long maxAge = 3600;
}
