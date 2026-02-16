package ua.kpi.sc.notification.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/**
 * Request DTO for marking notifications as read.
 *
 * @since 0.5.0
 */
public record MarkReadRequest(
        @NotEmpty List<UUID> ids
) {
}
