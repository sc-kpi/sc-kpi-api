package ua.kpi.sc.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full user details response including partner roles.
 *
 * @since 0.2.0
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        int capabilityTier,
        boolean active,
        Instant createdAt,
        List<PartnerMemberResponse> partnerRoles
) {
}
