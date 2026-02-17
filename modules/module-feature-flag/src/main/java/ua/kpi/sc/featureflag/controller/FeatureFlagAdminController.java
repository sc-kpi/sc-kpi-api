package ua.kpi.sc.featureflag.controller;

import java.util.List;
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
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireMfa;
import ua.kpi.sc.common.security.RequireTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.featureflag.dto.BulkToggleRequest;
import ua.kpi.sc.featureflag.dto.CreateFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.CreateOverrideRequest;
import ua.kpi.sc.featureflag.dto.FeatureFlagResponse;
import ua.kpi.sc.featureflag.dto.OverrideResponse;
import ua.kpi.sc.featureflag.dto.ToggleFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.UpdateFeatureFlagRequest;
import ua.kpi.sc.featureflag.service.FeatureFlagService;

@RestController
@RequestMapping("/api/v1/admin/feature-flags")
@Tag(name = "Feature Flags Admin", description = "Feature flag management endpoints (admin only)")
@RequireTier(CapabilityTier.ADMIN)
@RequireMfa
@RequiredArgsConstructor
public class FeatureFlagAdminController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public Page<FeatureFlagResponse> listFlags(Pageable pageable) {
        return featureFlagService.listFlags(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeatureFlagResponse createFlag(
            @Valid @RequestBody CreateFeatureFlagRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return featureFlagService.createFlag(request, requester);
    }

    @GetMapping("/{id}")
    public FeatureFlagResponse getFlag(@PathVariable UUID id) {
        return featureFlagService.getFlag(id);
    }

    @PatchMapping("/{id}")
    public FeatureFlagResponse updateFlag(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeatureFlagRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return featureFlagService.updateFlag(id, request, requester);
    }

    @PatchMapping("/{id}/toggle")
    public FeatureFlagResponse toggleFlag(
            @PathVariable UUID id,
            @Valid @RequestBody ToggleFeatureFlagRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return featureFlagService.toggleFlag(id, request, requester);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFlag(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal requester) {
        featureFlagService.deleteFlag(id, requester);
    }

    @PostMapping("/{id}/overrides")
    @ResponseStatus(HttpStatus.CREATED)
    public OverrideResponse addOverride(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOverrideRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return featureFlagService.addOverride(id, request, requester);
    }

    @DeleteMapping("/{id}/overrides/{overrideId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeOverride(
            @PathVariable UUID id,
            @PathVariable UUID overrideId,
            @AuthenticationPrincipal UserPrincipal requester) {
        featureFlagService.removeOverride(id, overrideId, requester);
    }

    @PostMapping("/bulk-toggle")
    public List<FeatureFlagResponse> bulkToggle(
            @Valid @RequestBody BulkToggleRequest request,
            @AuthenticationPrincipal UserPrincipal requester) {
        return featureFlagService.bulkToggle(request, requester);
    }

}
