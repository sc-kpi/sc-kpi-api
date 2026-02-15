package ua.kpi.sc.auth.util;

import org.springframework.http.ResponseCookie;
import ua.kpi.sc.common.security.SecurityConstants;

/**
 * Utility for creating HTTP-only cookies for authentication tokens.
 *
 * @since 0.1.0
 */
public final class CookieUtil {

    private CookieUtil() {
    }

    public static ResponseCookie createAccessTokenCookie(String token, long maxAgeMs, boolean secure) {
        return buildCookie(SecurityConstants.ACCESS_TOKEN_COOKIE, token, maxAgeMs / 1000, "/", secure);
    }

    public static ResponseCookie createRefreshTokenCookie(String token, long maxAgeMs, boolean secure) {
        return buildCookie(SecurityConstants.REFRESH_TOKEN_COOKIE, token, maxAgeMs / 1000, "/", secure);
    }

    public static ResponseCookie createDeleteCookie(String name, boolean secure) {
        return createDeleteCookie(name, "/", secure);
    }

    public static ResponseCookie createDeleteCookie(String name, String path, boolean secure) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(0)
                .build();
    }

    public static ResponseCookie buildCookie(String name, String value, long maxAgeSec, String path,
            boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAgeSec)
                .build();
    }
}
