package ua.kpi.sc.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Google's token endpoint.
 *
 * @since 0.1.0
 */
public record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("scope") String scope
) {
}
