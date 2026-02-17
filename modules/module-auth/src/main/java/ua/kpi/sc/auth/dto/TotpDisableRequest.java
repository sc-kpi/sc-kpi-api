package ua.kpi.sc.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for disabling TOTP 2FA (requires password + current TOTP code).
 *
 * @since 0.6.0
 */
public record TotpDisableRequest(
        @NotBlank(message = "Password is required")
        String password,
        @NotBlank(message = "Code is required")
        String code
) {
}
