package ua.kpi.sc.auth.dto;

import java.time.Instant;

/**
 * Response containing the current 2FA status for a user.
 *
 * @since 0.6.0
 */
public record TotpStatusResponse(
        boolean enabled,
        int recoveryCodesRemaining,
        Instant enabledAt
) {
}
