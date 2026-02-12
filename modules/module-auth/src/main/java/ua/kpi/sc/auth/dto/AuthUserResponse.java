package ua.kpi.sc.auth.dto;

import java.util.UUID;

/**
 * Response body representing the authenticated user's profile info.
 * Mirrors the frontend {@code AuthUser} type.
 *
 * @since 0.1.0
 */
public record AuthUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        int capabilityTier
) {
}
