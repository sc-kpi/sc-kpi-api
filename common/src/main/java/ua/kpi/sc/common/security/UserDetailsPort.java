package ua.kpi.sc.common.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Hexagonal architecture port for user identity operations.
 *
 * <p>This interface decouples the security layer from the persistence layer,
 * allowing the auth module to load and manage user credentials without depending
 * on entity or repository details.
 *
 * @see UserPrincipal
 * @since 0.1.0
 */
public interface UserDetailsPort {

    /**
     * Loads a user principal by email address.
     *
     * @param email the user's email
     * @return the principal if found, or empty
     */
    Optional<UserPrincipal> loadByEmail(String email);

    /**
     * Loads a user principal by unique identifier.
     *
     * @param id the user's UUID
     * @return the principal if found, or empty
     */
    Optional<UserPrincipal> loadById(UUID id);

    /**
     * Checks whether a user with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if a user with this email exists
     */
    boolean existsByEmail(String email);

    /**
     * Creates a new user with the given credentials and profile information.
     *
     * @param email        the user's email (must be unique)
     * @param passwordHash the BCrypt-hashed password
     * @param firstName    the user's first name
     * @param lastName     the user's last name
     * @return the created user principal
     */
    UserPrincipal createUser(String email, String passwordHash, String firstName, String lastName);

    /**
     * Updates the password for an existing user.
     *
     * @param userId       the user's UUID
     * @param passwordHash the new BCrypt-hashed password
     */
    void updatePassword(UUID userId, String passwordHash);

    /**
     * Assigns a partner access level to a user.
     *
     * @param userId     the user's UUID
     * @param partnerId  the partner organization UUID
     * @param level      the access level to assign
     * @param assignedBy the UUID of the admin performing the assignment
     */
    void assignPartnerLevel(UUID userId, UUID partnerId, PartnerLevel level, UUID assignedBy);

    /**
     * Removes a user's partner access level.
     *
     * @param userId    the user's UUID
     * @param partnerId the partner organization UUID
     */
    void removePartnerLevel(UUID userId, UUID partnerId);

    /**
     * Updates a user's capability tier.
     *
     * @param userId the user's UUID
     * @param tier   the new capability tier
     */
    void updateUserTier(UUID userId, CapabilityTier tier);

    /**
     * Updates a user's active status.
     *
     * @param userId the user's UUID
     * @param active whether the user should be active
     */
    void updateUserActiveStatus(UUID userId, boolean active);

    /**
     * Updates a user's profile fields (first name, last name).
     *
     * @param userId    the user's UUID
     * @param firstName the new first name
     * @param lastName  the new last name
     * @return the updated principal if found, or empty
     */
    Optional<UserPrincipal> updateUserProfile(UUID userId, String firstName, String lastName);
}
