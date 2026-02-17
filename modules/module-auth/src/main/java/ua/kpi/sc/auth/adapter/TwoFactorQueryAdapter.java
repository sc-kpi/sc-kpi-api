package ua.kpi.sc.auth.adapter;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.auth.repository.TotpSecretRepository;
import ua.kpi.sc.common.security.TwoFactorQueryPort;

/**
 * Adapter implementing {@link TwoFactorQueryPort} using JPA persistence.
 * Allows the user module to query 2FA status without depending on auth entities.
 *
 * @since 0.6.0
 */
@Component
@RequiredArgsConstructor
public class TwoFactorQueryAdapter implements TwoFactorQueryPort {

    private final TotpSecretRepository totpSecretRepository;

    @Override
    public boolean isTwoFactorEnabled(UUID userId) {
        return totpSecretRepository.existsByUserIdAndEnabledTrue(userId);
    }
}
