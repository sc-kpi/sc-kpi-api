package ua.kpi.sc.user.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for assigning a partner access level to a user.
 *
 * @since 0.2.0
 */
public record AssignPartnerLevelRequest(
        @NotNull UUID partnerId,
        @NotBlank String level
) {
}
