package ua.kpi.sc.common.security;

import java.util.List;
import java.util.UUID;

/**
 * Port interface for querying user information needed by modules that cannot
 * depend directly on module-user (e.g. module-notification for broadcast resolution).
 *
 * @since 0.5.0
 */
public interface UserQueryPort {

    /**
     * Returns IDs of all active users whose tier is at or above the given minimum.
     */
    List<UUID> findActiveUserIdsByMinimumTier(CapabilityTier minimumTier);

    /**
     * Returns IDs of all active users.
     */
    List<UUID> findAllActiveUserIds();

    /**
     * Returns the email address for a user, or {@code null} if the user does not exist.
     */
    String getEmailById(UUID userId);
}
