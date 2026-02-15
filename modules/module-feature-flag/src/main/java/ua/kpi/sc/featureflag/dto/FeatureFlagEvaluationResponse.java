package ua.kpi.sc.featureflag.dto;

public record FeatureFlagEvaluationResponse(
        String key,
        boolean enabled
) {
}
