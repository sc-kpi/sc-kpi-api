package ua.kpi.sc.auth.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.auth.entity.TotpRecoveryCode;

/**
 * Spring Data JPA repository for {@link TotpRecoveryCode} entities.
 *
 * @since 0.6.0
 */
public interface TotpRecoveryCodeRepository extends JpaRepository<TotpRecoveryCode, UUID> {

    List<TotpRecoveryCode> findByUserIdAndUsedFalse(UUID userId);

    int countByUserIdAndUsedFalse(UUID userId);

    void deleteByUserId(UUID userId);
}
