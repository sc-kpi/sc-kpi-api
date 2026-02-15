package ua.kpi.sc.featureflag.config;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.kpi.sc.common.featureflag.FeatureFlagChecker;
import ua.kpi.sc.featureflag.service.FeatureFlagEvaluationService;

@Component
@RequiredArgsConstructor
public class FeatureFlagCheckerAdapter implements FeatureFlagChecker {

    private final FeatureFlagEvaluationService evaluationService;

    @Override
    public boolean isEnabled(String key, UUID userId, Integer tierLevel) {
        return evaluationService.evaluate(key, userId, tierLevel);
    }
}
