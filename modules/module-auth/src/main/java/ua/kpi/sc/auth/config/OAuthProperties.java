package ua.kpi.sc.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized Google OAuth 2.0 configuration bound to the {@code app.oauth2.google} prefix.
 *
 * <p>Setup instructions:
 * <ol>
 *   <li>Go to <a href="https://console.cloud.google.com/">Google Cloud Console</a></li>
 *   <li>Create or select a project</li>
 *   <li>Navigate to APIs &amp; Services &rarr; Credentials</li>
 *   <li>Create Credentials &rarr; OAuth client ID (Web application)</li>
 *   <li>Set Authorized redirect URIs to the {@code redirect-uri} value</li>
 *   <li>Set {@code GOOGLE_CLIENT_ID} and {@code GOOGLE_CLIENT_SECRET} environment variables</li>
 * </ol>
 *
 * @since 0.1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.oauth2.google")
public class OAuthProperties {
    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "http://localhost:8080/api/v1/auth/oauth2/callback/google";
    private String frontendUrl = "http://localhost:3000";
}
