package ua.kpi.sc.featureflag.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.BadRequestException;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.featureflag.dto.BulkToggleRequest;
import ua.kpi.sc.featureflag.dto.CreateFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.CreateOverrideRequest;
import ua.kpi.sc.featureflag.dto.FeatureFlagResponse;
import ua.kpi.sc.featureflag.dto.OverrideResponse;
import ua.kpi.sc.featureflag.dto.ToggleFeatureFlagRequest;
import ua.kpi.sc.featureflag.dto.UpdateFeatureFlagRequest;
import ua.kpi.sc.featureflag.entity.FeatureFlag;
import ua.kpi.sc.featureflag.entity.FeatureFlagOverride;
import ua.kpi.sc.featureflag.entity.OverrideType;
import ua.kpi.sc.featureflag.repository.FeatureFlagOverrideRepository;
import ua.kpi.sc.featureflag.repository.FeatureFlagRepository;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagOverrideRepository overrideRepository;
    private final AuditPublisher auditPublisher;
    private final FeatureFlagEvaluationService evaluationService;

    @Transactional(readOnly = true)
    public Page<FeatureFlagResponse> listFlags(Pageable pageable) {
        return flagRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FeatureFlagResponse getFlag(UUID id) {
        return toResponse(findFlagById(id));
    }

    @Transactional(readOnly = true)
    public FeatureFlagResponse getFlagByKey(String key) {
        return toResponse(flagRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", key)));
    }

    @Transactional
    public FeatureFlagResponse createFlag(CreateFeatureFlagRequest request, UserPrincipal requester) {
        if (flagRepository.existsByKey(request.key())) {
            throw new ConflictException("Feature flag with key '%s' already exists".formatted(request.key()));
        }

        FeatureFlag flag = FeatureFlag.builder()
                .key(request.key())
                .name(request.name())
                .description(request.description())
                .enabled(request.enabled())
                .environment(request.environment())
                .rolloutPercentage(request.rolloutPercentage())
                .createdBy(requester.getId())
                .build();

        FeatureFlag saved = flagRepository.save(flag);
        publishAudit(saved.getId(), saved.getKey(), AuditAction.CREATED, null, null,
                String.valueOf(saved.isEnabled()), null, requester);
        evaluationService.evictCache();
        return toResponse(saved);
    }

    @Transactional
    public FeatureFlagResponse updateFlag(UUID id, UpdateFeatureFlagRequest request, UserPrincipal requester) {
        FeatureFlag flag = findFlagById(id);

        if (request.name() != null) {
            publishFieldChange(flag, "name", flag.getName(), request.name(), requester);
            flag.setName(request.name());
        }
        if (request.description() != null) {
            publishFieldChange(flag, "description", flag.getDescription(), request.description(), requester);
            flag.setDescription(request.description());
        }
        if (request.enabled() != null) {
            publishFieldChange(flag, "enabled", String.valueOf(flag.isEnabled()),
                    String.valueOf(request.enabled()), requester);
            flag.setEnabled(request.enabled());
        }
        if (request.environment() != null) {
            publishFieldChange(flag, "environment", flag.getEnvironment(), request.environment(), requester);
            flag.setEnvironment(request.environment());
        }
        if (request.rolloutPercentage() != null) {
            publishFieldChange(flag, "rolloutPercentage", String.valueOf(flag.getRolloutPercentage()),
                    String.valueOf(request.rolloutPercentage()), requester);
            flag.setRolloutPercentage(request.rolloutPercentage());
        }

        FeatureFlag saved = flagRepository.save(flag);
        evaluationService.evictCache();
        return toResponse(saved);
    }

    @Transactional
    public FeatureFlagResponse toggleFlag(UUID id, ToggleFeatureFlagRequest request, UserPrincipal requester) {
        FeatureFlag flag = findFlagById(id);
        String oldValue = String.valueOf(flag.isEnabled());
        String newValue = String.valueOf(request.enabled());

        flag.setEnabled(request.enabled());
        FeatureFlag saved = flagRepository.save(flag);

        publishAudit(saved.getId(), saved.getKey(), AuditAction.TOGGLED, "enabled", oldValue, newValue,
                request.reason(), requester);
        evaluationService.evictCache();
        return toResponse(saved);
    }

    @Transactional
    public void deleteFlag(UUID id, UserPrincipal requester) {
        FeatureFlag flag = findFlagById(id);
        publishAudit(flag.getId(), flag.getKey(), AuditAction.DELETED, null, null, null, null, requester);
        flagRepository.delete(flag);
        evaluationService.evictCache();
    }

    @Transactional
    public OverrideResponse addOverride(UUID flagId, CreateOverrideRequest request, UserPrincipal requester) {
        FeatureFlag flag = findFlagById(flagId);
        validateOverrideRequest(request);

        FeatureFlagOverride override = FeatureFlagOverride.builder()
                .flag(flag)
                .overrideType(request.overrideType())
                .tierLevel(request.tierLevel())
                .userId(request.userId())
                .enabled(request.enabled())
                .build();

        FeatureFlagOverride saved = overrideRepository.save(override);
        publishAudit(flag.getId(), flag.getKey(), AuditAction.OVERRIDE_ADDED, request.overrideType().name(),
                null, String.valueOf(request.enabled()), null, requester);
        evaluationService.evictCache();
        return toOverrideResponse(saved);
    }

    @Transactional
    public void removeOverride(UUID flagId, UUID overrideId, UserPrincipal requester) {
        FeatureFlag flag = findFlagById(flagId);
        FeatureFlagOverride override = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagOverride", overrideId));

        if (!override.getFlag().getId().equals(flagId)) {
            throw new BadRequestException("Override does not belong to the specified flag");
        }

        publishAudit(flag.getId(), flag.getKey(), AuditAction.OVERRIDE_REMOVED, override.getOverrideType().name(),
                String.valueOf(override.isEnabled()), null, null, requester);
        overrideRepository.delete(override);
        evaluationService.evictCache();
    }

    @Transactional
    public List<FeatureFlagResponse> bulkToggle(BulkToggleRequest request, UserPrincipal requester) {
        return request.keys().stream()
                .map(key -> {
                    var flag = flagRepository.findByKey(key)
                            .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", key));
                    String oldValue = String.valueOf(flag.isEnabled());
                    flag.setEnabled(request.enabled());
                    var saved = flagRepository.save(flag);
                    publishAudit(saved.getId(), saved.getKey(), AuditAction.BULK_TOGGLED, "enabled",
                            oldValue, String.valueOf(request.enabled()), request.reason(), requester);
                    return toResponse(saved);
                })
                .toList();
    }

    private FeatureFlag findFlagById(UUID id) {
        return flagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", id));
    }

    private void validateOverrideRequest(CreateOverrideRequest request) {
        if (request.overrideType() == OverrideType.TIER && request.tierLevel() == null) {
            throw new BadRequestException("tierLevel is required for TIER override");
        }
        if (request.overrideType() == OverrideType.USER && request.userId() == null) {
            throw new BadRequestException("userId is required for USER override");
        }
    }

    private void publishFieldChange(FeatureFlag flag, String fieldName, String oldValue,
                                    String newValue, UserPrincipal requester) {
        publishAudit(flag.getId(), flag.getKey(), AuditAction.UPDATED, fieldName, oldValue, newValue, null, requester);
    }

    private void publishAudit(UUID flagId, String flagKey, AuditAction action, String fieldName,
                              String oldValue, String newValue, String reason, UserPrincipal requester) {
        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(requester)
                .action(action)
                .entityType(AuditEntityType.FEATURE_FLAG)
                .entityId(flagId)
                .entityName(flagKey)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .details(reason)
                .sourceModule("feature-flag")
                .build());
    }

    private FeatureFlagResponse toResponse(FeatureFlag flag) {
        var overrideResponses = flag.getOverrides().stream()
                .map(this::toOverrideResponse)
                .toList();

        return new FeatureFlagResponse(
                flag.getId(),
                flag.getKey(),
                flag.getName(),
                flag.getDescription(),
                flag.isEnabled(),
                flag.getEnvironment(),
                flag.getRolloutPercentage(),
                overrideResponses,
                flag.getCreatedBy(),
                flag.getCreatedAt(),
                flag.getUpdatedAt()
        );
    }

    private OverrideResponse toOverrideResponse(FeatureFlagOverride override) {
        return new OverrideResponse(
                override.getId(),
                override.getOverrideType(),
                override.getTierLevel(),
                override.getUserId(),
                override.isEnabled()
        );
    }
}
