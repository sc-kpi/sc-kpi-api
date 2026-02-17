package ua.kpi.sc.ratelimit.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireMfa;
import ua.kpi.sc.common.security.RequireTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.ratelimit.dto.CreateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.dto.RateLimitRuleResponse;
import ua.kpi.sc.ratelimit.dto.RateLimitStatsResponse;
import ua.kpi.sc.ratelimit.dto.RateLimitToggleRequest;
import ua.kpi.sc.ratelimit.dto.UpdateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.service.RateLimitService;

@RestController
@RequestMapping("/api/v1/admin/rate-limits")
@Tag(name = "Rate Limits Admin", description = "Rate limit rule management endpoints (admin only)")
@RequireTier(CapabilityTier.ADMIN)
@RequireMfa
@FeatureFlag("rate-limiting.admin")
@RequiredArgsConstructor
public class RateLimitAdminController {

    private final RateLimitService rateLimitService;

    @GetMapping
    public Page<RateLimitRuleResponse> listRules(Pageable pageable) {
        return rateLimitService.listRules(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RateLimitRuleResponse createRule(
            @Valid @RequestBody CreateRateLimitRuleRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return rateLimitService.createRule(request, requester);
    }

    @GetMapping("/{id}")
    public RateLimitRuleResponse getRule(@PathVariable UUID id) {
        return rateLimitService.getRule(id);
    }

    @PatchMapping("/{id}")
    public RateLimitRuleResponse updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRateLimitRuleRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return rateLimitService.updateRule(id, request, requester);
    }

    @PatchMapping("/{id}/toggle")
    public RateLimitRuleResponse toggleRule(
            @PathVariable UUID id,
            @Valid @RequestBody RateLimitToggleRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return rateLimitService.toggleRule(id, request, requester);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal requester) {
        rateLimitService.deleteRule(id, requester);
    }

    @GetMapping("/stats")
    public RateLimitStatsResponse getStats() {
        return rateLimitService.getStats();
    }
}
