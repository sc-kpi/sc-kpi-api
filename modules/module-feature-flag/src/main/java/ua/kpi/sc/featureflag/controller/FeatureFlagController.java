package ua.kpi.sc.featureflag.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.featureflag.dto.FeatureFlagEvaluationResponse;
import ua.kpi.sc.featureflag.service.FeatureFlagEvaluationService;

@RestController
@RequestMapping("/api/v1/feature-flags")
@Tag(name = "Feature Flags", description = "Feature flag evaluation endpoints")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagEvaluationService evaluationService;

    @GetMapping
    public Map<String, Boolean> evaluateAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return evaluationService.evaluateAll(null, null);
        }
        return evaluationService.evaluateAll(principal.getId(), principal.getTier().getLevel());
    }

    @GetMapping("/{key}")
    public FeatureFlagEvaluationResponse evaluate(
            @PathVariable String key,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean enabled;
        if (principal == null) {
            enabled = evaluationService.evaluate(key, null, null);
        } else {
            enabled = evaluationService.evaluate(key, principal.getId(), principal.getTier().getLevel());
        }
        return new FeatureFlagEvaluationResponse(key, enabled);
    }
}
