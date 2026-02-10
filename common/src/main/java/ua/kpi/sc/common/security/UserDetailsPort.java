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
}
