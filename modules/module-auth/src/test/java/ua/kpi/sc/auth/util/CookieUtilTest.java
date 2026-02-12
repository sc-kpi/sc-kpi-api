package ua.kpi.sc.auth.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import ua.kpi.sc.common.security.SecurityConstants;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    @Test
    void accessTokenCookieHasCorrectAttributes() {
        ResponseCookie cookie = CookieUtil.createAccessTokenCookie("token123", 3600000);

        assertThat(cookie.getName()).isEqualTo(SecurityConstants.ACCESS_TOKEN_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("token123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600);
    }

    @Test
    void refreshTokenCookieHasCorrectAttributes() {
        ResponseCookie cookie = CookieUtil.createRefreshTokenCookie("refresh123", 2592000000L);

        assertThat(cookie.getName()).isEqualTo(SecurityConstants.REFRESH_TOKEN_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("refresh123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(2592000);
    }

    @Test
    void deleteCookieHasZeroMaxAge() {
        ResponseCookie cookie = CookieUtil.createDeleteCookie("access_token");

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
    }
}
