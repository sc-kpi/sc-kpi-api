package ua.kpi.sc.user.dto;

/**
 * Request body for updating a user's active status.
 *
 * @since 0.2.0
 */
public record UpdateStatusRequest(
        boolean active
) {
}
