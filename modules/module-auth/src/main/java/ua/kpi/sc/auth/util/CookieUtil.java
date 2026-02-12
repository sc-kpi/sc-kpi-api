package ua.kpi.sc.auth.util;

import org.springframework.http.ResponseCookie;
import ua.kpi.sc.common.security.SecurityConstants;

/**
 * Utility for creating secure HTTP-only cookies for authentication tokens.
 *
 * @since 0.1.0
 */
public final class CookieUtil {

    private CookieUtil() {
    }

    public static ResponseCookie createAccessTokenCookie(String token, long maxAgeMs) {
        return buildCookie(SecurityConstants.ACCESS_TOKEN_COOKIE, token, maxAgeMs / 1000);
    }

    public static ResponseCookie createRefreshTokenCookie(String token, long maxAgeMs) {
        return buildCookie(SecurityConstants.REFRESH_TOKEN_COOKIE, token, maxAgeMs / 1000);
    }

    public static ResponseCookie createDeleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private static ResponseCookie buildCookie(String name, String value, long maxAgeSec) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSec)
                .build();
    }
}
