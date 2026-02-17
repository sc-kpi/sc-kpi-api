package ua.kpi.sc.common.security;

/**
 * Security-related constants shared across modules.
 *
 * <p>Note: {@link #PUBLIC_URLS} defines URL patterns intended for unauthenticated access,
 * but enforcement depends on the security filter chain configuration in the auth module.
 *
 * @since 0.1.0
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** HTTP header name for Bearer token authentication. */
    public static final String AUTH_HEADER = "Authorization";
    /** Prefix for Bearer tokens in the Authorization header. */
    public static final String TOKEN_PREFIX = "Bearer ";
    /** Cookie name for the JWT access token. */
    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    /** Cookie name for the JWT refresh token. */
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    /** Cookie name for the short-lived MFA challenge token. */
    public static final String MFA_TOKEN_COOKIE = "mfa_token";

    /**
     * URL patterns that should be accessible without authentication.
     * Enforcement depends on the security filter chain configuration.
     */
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/api/v1/clubs",
            "/api/v1/clubs/**",
            "/api/v1/projects",
            "/api/v1/projects/**",
            "/api/v1/departments",
            "/api/v1/departments/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };
}
