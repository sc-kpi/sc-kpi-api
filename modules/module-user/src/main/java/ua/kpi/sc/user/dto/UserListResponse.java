package ua.kpi.sc.user.dto;

import java.util.UUID;

/**
 * Lightweight user response for paginated lists.
 *
 * @since 0.2.0
 */
public record UserListResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        int capabilityTier,
        boolean active
) {
}
