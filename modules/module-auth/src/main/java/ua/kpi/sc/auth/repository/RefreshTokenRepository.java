package ua.kpi.sc.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.auth.entity.RefreshToken;

/**
 * Spring Data JPA repository for {@link RefreshToken} entities.
 *
 * @since 0.1.0
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);

    void deleteByExpiresAtBefore(Instant instant);
}
