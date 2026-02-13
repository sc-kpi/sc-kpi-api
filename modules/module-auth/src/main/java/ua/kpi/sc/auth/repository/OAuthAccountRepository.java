package ua.kpi.sc.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.auth.entity.OAuthAccount;

/**
 * Spring Data JPA repository for OAuth accounts.
 *
 * @since 0.1.0
 */
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<OAuthAccount> findByUserId(UUID userId);
}
