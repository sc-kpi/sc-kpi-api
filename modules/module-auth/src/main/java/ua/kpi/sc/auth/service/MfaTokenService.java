package ua.kpi.sc.auth.service;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.kpi.sc.auth.config.JwtProperties;
import ua.kpi.sc.auth.config.MfaProperties;

/**
 * Generates and validates short-lived MFA challenge JWTs.
 * These tokens are issued after successful password verification when 2FA is enabled,
 * and are required to complete the login via the TOTP verification endpoint.
 *
 * @since 0.6.0
 */
@Slf4j
@Service
public class MfaTokenService {

    private static final String MFA_TOKEN_TYPE = "mfa";

    private final SecretKey secretKey;
    private final long tokenExpiration;

    public MfaTokenService(JwtProperties jwtProperties, MfaProperties mfaProperties) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.tokenExpiration = mfaProperties.getTokenExpiration();
    }

    /**
     * Generates a short-lived MFA challenge token for the given user.
     *
     * @param userId the user's UUID
     * @return the signed JWT string
     */
    public String generateMfaToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + tokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("type", MFA_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates an MFA token and extracts the user ID.
     *
     * @param token the MFA JWT string
     * @return the user's UUID if valid
     * @throws IllegalArgumentException if the token is invalid or not an MFA token
     */
    public UUID validateAndExtractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!MFA_TOKEN_TYPE.equals(type)) {
                throw new IllegalArgumentException("Not an MFA token");
            }

            return UUID.fromString(claims.getSubject());
        } catch (JwtException e) {
            log.debug("Invalid MFA token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired MFA token", e);
        }
    }
}
