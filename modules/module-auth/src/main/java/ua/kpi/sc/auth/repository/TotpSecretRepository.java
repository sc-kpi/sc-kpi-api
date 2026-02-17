package ua.kpi.sc.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.auth.entity.TotpSecret;

/**
 * Spring Data JPA repository for {@link TotpSecret} entities.
 *
 * @since 0.6.0
 */
public interface TotpSecretRepository extends JpaRepository<TotpSecret, UUID> {

    Optional<TotpSecret> findByUserId(UUID userId);

    boolean existsByUserIdAndEnabledTrue(UUID userId);

    void deleteByUserId(UUID userId);

    long countByEnabledTrue();
}
