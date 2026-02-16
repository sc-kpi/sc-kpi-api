package ua.kpi.sc.common.notification;

import java.util.UUID;

import ua.kpi.sc.common.security.CapabilityTier;

/**
 * Port interface for publishing notification events from any module.
 * <p>
 * Implementations must handle delivery asynchronously and never throw exceptions
 * to the caller (fire-and-forget pattern).
 *
 * @since 0.5.0
 */
public interface NotificationPublisher {

    /**
     * Publishes a notification to a specific user.
     */
    void publishToUser(UUID userId, NotificationEvent event);

    /**
     * Publishes a notification to all active users at or above the given tier.
     */
    void publishToTier(CapabilityTier tier, NotificationEvent event);

    /**
     * Publishes a notification to all active users.
     */
    void publishToAll(NotificationEvent event);
}
