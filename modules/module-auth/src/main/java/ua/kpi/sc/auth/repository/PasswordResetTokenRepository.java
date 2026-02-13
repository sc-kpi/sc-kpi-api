package ua.kpi.sc.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.auth.entity.PasswordResetToken;

/**
 * Spring Data JPA repository for password reset tokens.
 *
 * @since 0.1.0
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);

    void deleteByExpiresAtBefore(Instant instant);
}
