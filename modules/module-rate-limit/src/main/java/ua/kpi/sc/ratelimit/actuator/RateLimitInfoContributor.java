package ua.kpi.sc.ratelimit.actuator;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.ratelimit.config.RateLimitProperties;
import ua.kpi.sc.ratelimit.repository.RateLimitRuleRepository;
import ua.kpi.sc.ratelimit.service.RateLimitEvaluationService;

@Component
@RequiredArgsConstructor
public class RateLimitInfoContributor implements InfoContributor {

    private final RateLimitRuleRepository ruleRepository;
    private final RateLimitProperties properties;
    private final RateLimitEvaluationService evaluationService;

    @Override
    public void contribute(Info.Builder builder) {
        var allRules = ruleRepository.findAll();

        builder.withDetail("rateLimit", Map.of(
                "enabled", properties.isEnabled(),
                "storageType", properties.getStorageType(),
                "totalRules", allRules.size(),
                "enabledRules", allRules.stream().filter(r -> r.isEnabled()).count(),
                "activeBuckets", evaluationService.getActiveBuckets(),
                "totalViolations", evaluationService.getTotalViolations()
        ));
    }
}
