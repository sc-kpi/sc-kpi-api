package ua.kpi.sc.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for TOTP verification (login or setup).
 *
 * @since 0.6.0
 */
public record TotpVerifyRequest(
        @NotBlank(message = "Code is required")
        String code
) {
}
