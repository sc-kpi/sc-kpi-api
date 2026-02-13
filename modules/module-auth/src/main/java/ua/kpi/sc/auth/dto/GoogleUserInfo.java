package ua.kpi.sc.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Google's userinfo endpoint.
 *
 * @since 0.1.0
 */
public record GoogleUserInfo(
        String sub,
        String email,
        @JsonProperty("email_verified") boolean emailVerified,
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName,
        String picture
) {
}
