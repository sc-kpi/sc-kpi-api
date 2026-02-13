package ua.kpi.sc.user.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body representing a user's partner membership.
 *
 * @since 0.2.0
 */
public record PartnerMemberResponse(
        UUID partnerId,
        String level,
        Instant assignedAt
) {
}
