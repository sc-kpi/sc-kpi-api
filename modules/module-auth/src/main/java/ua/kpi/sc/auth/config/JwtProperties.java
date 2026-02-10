package ua.kpi.sc.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized JWT configuration bound to the {@code jwt.*} property prefix.
 *
 * <p>Token expiration durations:
 * <ul>
 *   <li>{@code jwt.access-expiration} — access token lifetime, default 1 hour (3 600 000 ms)</li>
 *   <li>{@code jwt.refresh-expiration} — refresh token lifetime, default 30 days (2 592 000 000 ms)</li>
 *   <li>{@code jwt.remember-me-refresh-expiration} — "remember me" refresh token, default 90 days (7 776 000 000 ms)</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessExpiration = 3600000; // 1 hour
    private long refreshExpiration = 2592000000L; // 30 days
    private long rememberMeRefreshExpiration = 7776000000L; // 90 days
}
