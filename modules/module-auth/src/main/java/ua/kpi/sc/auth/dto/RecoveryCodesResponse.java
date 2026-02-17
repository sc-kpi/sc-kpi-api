package ua.kpi.sc.auth.dto;

import java.util.List;

/**
 * Response containing plaintext recovery codes (shown once to the user).
 *
 * @since 0.6.0
 */
public record RecoveryCodesResponse(
        List<String> codes
) {
}
