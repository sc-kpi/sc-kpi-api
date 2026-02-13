package ua.kpi.sc.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request body for updating a user's capability tier.
 *
 * @since 0.2.0
 */
public record UpdateTierRequest(
        @Min(0) @Max(5) int tier
) {
}
