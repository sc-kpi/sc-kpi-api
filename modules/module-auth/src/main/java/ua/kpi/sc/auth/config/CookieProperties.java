package ua.kpi.sc.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized cookie configuration bound to the {@code cookie.*} property prefix.
 *
 * @since 0.1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {
    private boolean secure = true;
}
