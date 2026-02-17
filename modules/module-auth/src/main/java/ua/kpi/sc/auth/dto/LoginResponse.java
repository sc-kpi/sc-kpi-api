package ua.kpi.sc.auth.dto;

import java.util.List;
import java.util.UUID;

/**
 * Login response that extends AuthUserResponse with 2FA state.
 * When {@code twoFactorRequired} is true, the user field values are null
 * and the client must complete MFA verification.
 *
 * @since 0.6.0
 */
public record LoginResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        int capabilityTier,
        List<AuthUserResponse.PartnerRoleDto> partnerRoles,
        boolean twoFactorEnabled,
        boolean twoFactorRequired
) {

    /**
     * Creates a login response from a successful authentication (no MFA required).
     */
    public static LoginResponse fromAuthUser(AuthUserResponse user, boolean twoFactorEnabled) {
        return new LoginResponse(
                user.id(), user.email(), user.firstName(), user.lastName(),
                user.capabilityTier(), user.partnerRoles(),
                twoFactorEnabled, false
        );
    }

    /**
     * Creates a login response indicating MFA challenge is required.
     */
    public static LoginResponse mfaChallenge() {
        return new LoginResponse(null, null, null, null, 0, null, true, true);
    }
}
