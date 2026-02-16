package ua.kpi.sc.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ua.kpi.sc.common.notification.NotificationCategory;

/**
 * Request DTO for broadcasting a notification to users.
 *
 * @since 0.5.0
 */
public record BroadcastRequest(
        @NotBlank String titleKey,
        @NotBlank String bodyKey,
        String[] bodyArgs,
        @NotNull NotificationCategory category,
        String targetTier
) {
}
