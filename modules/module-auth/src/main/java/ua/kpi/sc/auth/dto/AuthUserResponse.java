package ua.kpi.sc.auth.dto;

import java.util.List;
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
        int capabilityTier,
        List<PartnerRoleDto> partnerRoles,
        boolean twoFactorEnabled
) {

    /**
     * Partner role assignment for the authenticated user.
     *
     * @param partnerId the partner organization UUID
     * @param level     the access level (full, documents, basic)
     */
    public record PartnerRoleDto(UUID partnerId, String level) {
    }
}
