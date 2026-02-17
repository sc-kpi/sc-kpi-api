package ua.kpi.sc.auth.actuator;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.auth.config.MfaProperties;
import ua.kpi.sc.auth.service.TotpService;

/**
 * Actuator {@link InfoContributor} that exposes MFA statistics at {@code /actuator/info}.
 *
 * @since 0.6.0
 */
@Component
@RequiredArgsConstructor
public class MfaInfoContributor implements InfoContributor {

    private final TotpService totpService;
    private final MfaProperties mfaProperties;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("mfa", Map.of(
                "totpEnabled", true,
                "usersWithMfa", totpService.countUsersWithMfa(),
                "enforcement", Map.of(
                        "requiredTiers", mfaProperties.getEnforcement().getRequiredTiers(),
                        "gracePeriodDays", mfaProperties.getEnforcement().getGracePeriodDays()
                )
        ));
    }
}
