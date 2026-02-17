package ua.kpi.sc.common.security;

import java.util.UUID;

/**
 * Hexagonal architecture port for querying two-factor authentication status.
 *
 * <p>This interface decouples the user module from the auth module,
 * allowing {@link UserPrincipal} to include 2FA state without a direct
 * dependency on TOTP repositories.
 *
 * @since 0.6.0
 */
public interface TwoFactorQueryPort {

    /**
     * Checks whether two-factor authentication is enabled for the given user.
     *
     * @param userId the user's UUID
     * @return {@code true} if 2FA is active
     */
    boolean isTwoFactorEnabled(UUID userId);
}
