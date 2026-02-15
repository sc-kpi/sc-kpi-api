package ua.kpi.sc.featureflag.actuator;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.featureflag.config.FeatureFlagProperties;
import ua.kpi.sc.featureflag.repository.FeatureFlagRepository;

@Component
@RequiredArgsConstructor
public class FeatureFlagInfoContributor implements InfoContributor {

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagProperties properties;

    @Override
    public void contribute(Info.Builder builder) {
        var flags = flagRepository.findAll();

        Map<String, Boolean> flagStates = new HashMap<>();
        flags.forEach(f -> flagStates.put(f.getKey(), f.isEnabled()));

        builder.withDetail("featureFlags", Map.of(
                "totalFlags", flags.size(),
                "enabledFlags", flags.stream().filter(f -> f.isEnabled()).count(),
                "configOverrides", properties.getOverrides().size(),
                "flags", flagStates
        ));
    }
}
