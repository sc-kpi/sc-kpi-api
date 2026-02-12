package ua.kpi.sc.auth.security;

import java.util.UUID;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // base64 of "this is a very secure key for development purposes only"
        props.setSecret("dGhpcyBpcyBhIHZlcnkgc2VjdXJlIGtleSBmb3IgZGV2ZWxvcG1lbnQgcHVycG9zZXMgb25seQ==");
        props.setAccessExpiration(3600000);
        props.setRefreshExpiration(2592000000L);
        provider = new JwtTokenProvider(props);
    }

    private UserPrincipal testPrincipal() {
        return UserPrincipal.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .email("test@kpi.ua")
                .password("hashed")
                .firstName("Test")
                .lastName("User")
                .tier(CapabilityTier.BASIC)
                .active(true)
                .build();
    }

    @Test
    void generateAccessTokenContainsCorrectClaims() {
        String token = provider.generateAccessToken(testPrincipal());

        Claims claims = provider.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(claims.get("email", String.class)).isEqualTo("test@kpi.ua");
        assertThat(claims.get("tier", Integer.class)).isEqualTo(1);
    }

    @Test
    void accessTokenHasExpiration() {
        String token = provider.generateAccessToken(testPrincipal());
        Claims claims = provider.extractClaims(token);
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void validTokenPassesValidation() {
        String token = provider.generateAccessToken(testPrincipal());
        assertThat(provider.validateAccessToken(token)).isTrue();
    }

    @Test
    void tamperedTokenFailsValidation() {
        String token = provider.generateAccessToken(testPrincipal());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(provider.validateAccessToken(tampered)).isFalse();
    }

    @Test
    void nullTokenFailsValidation() {
        assertThat(provider.validateAccessToken(null)).isFalse();
    }

    @Test
    void extractUserIdFromValidToken() {
        String token = provider.generateAccessToken(testPrincipal());
        UUID userId = provider.extractUserId(token);
        assertThat(userId).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void generateRefreshTokenReturnsUuidString() {
        String token = provider.generateRefreshToken();
        assertThat(token).isNotBlank();
        assertThat(UUID.fromString(token)).isNotNull();
    }

    @Test
    void expiredTokenFailsValidation() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dGhpcyBpcyBhIHZlcnkgc2VjdXJlIGtleSBmb3IgZGV2ZWxvcG1lbnQgcHVycG9zZXMgb25seQ==");
        props.setAccessExpiration(-1000); // already expired
        JwtTokenProvider expiredProvider = new JwtTokenProvider(props);

        String token = expiredProvider.generateAccessToken(testPrincipal());
        assertThat(provider.validateAccessToken(token)).isFalse();
    }
}
