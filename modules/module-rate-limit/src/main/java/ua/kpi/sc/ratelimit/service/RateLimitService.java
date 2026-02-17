package ua.kpi.sc.ratelimit.service;

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
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationEventBuilder;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.ratelimit.dto.CreateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.dto.RateLimitRuleResponse;
import ua.kpi.sc.ratelimit.dto.RateLimitStatsResponse;
import ua.kpi.sc.ratelimit.dto.RateLimitToggleRequest;
import ua.kpi.sc.ratelimit.dto.UpdateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.entity.RateLimitRule;
import ua.kpi.sc.ratelimit.repository.RateLimitRuleRepository;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitRuleRepository ruleRepository;
    private final RateLimitEvaluationService evaluationService;
    private final AuditPublisher auditPublisher;
    private final NotificationPublisher notificationPublisher;

    @Transactional(readOnly = true)
    public Page<RateLimitRuleResponse> listRules(Pageable pageable) {
        return ruleRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RateLimitRuleResponse getRule(UUID id) {
        return toResponse(findRuleById(id));
    }

    @Transactional
    public RateLimitRuleResponse createRule(CreateRateLimitRuleRequest request, UserPrincipal requester) {
        if (ruleRepository.existsByName(request.name())) {
            throw new ConflictException("Rate limit rule with name '%s' already exists".formatted(request.name()));
        }

        RateLimitRule rule = RateLimitRule.builder()
                .name(request.name())
                .description(request.description())
                .endpointPattern(request.endpointPattern())
                .httpMethod(request.httpMethod())
                .limitPerPeriod(request.limitPerPeriod())
                .periodSeconds(request.periodSeconds())
                .burstCapacity(request.burstCapacity())
                .scope(request.scope())
                .targetTier(request.targetTier())
                .targetUserId(request.targetUserId())
                .timeWindowStart(request.timeWindowStart())
                .timeWindowEnd(request.timeWindowEnd())
                .priority(request.priority())
                .enabled(request.enabled())
                .createdBy(requester.getId())
                .build();

        RateLimitRule saved = ruleRepository.save(rule);
        publishAudit(saved.getId(), saved.getName(), AuditAction.CREATED, null, null,
                String.valueOf(saved.isEnabled()), null, requester);
        evaluationService.evictRulesCache();
        return toResponse(saved);
    }

    @Transactional
    public RateLimitRuleResponse updateRule(UUID id, UpdateRateLimitRuleRequest request, UserPrincipal requester) {
        RateLimitRule rule = findRuleById(id);

        if (request.description() != null) {
            publishFieldChange(rule, "description", rule.getDescription(), request.description(), requester);
            rule.setDescription(request.description());
        }
        if (request.endpointPattern() != null) {
            publishFieldChange(rule, "endpointPattern", rule.getEndpointPattern(), request.endpointPattern(), requester);
            rule.setEndpointPattern(request.endpointPattern());
        }
        if (request.httpMethod() != null) {
            publishFieldChange(rule, "httpMethod", rule.getHttpMethod(), request.httpMethod(), requester);
            rule.setHttpMethod(request.httpMethod());
        }
        if (request.limitPerPeriod() != null) {
            publishFieldChange(rule, "limitPerPeriod", String.valueOf(rule.getLimitPerPeriod()),
                    String.valueOf(request.limitPerPeriod()), requester);
            rule.setLimitPerPeriod(request.limitPerPeriod());
        }
        if (request.periodSeconds() != null) {
            publishFieldChange(rule, "periodSeconds", String.valueOf(rule.getPeriodSeconds()),
                    String.valueOf(request.periodSeconds()), requester);
            rule.setPeriodSeconds(request.periodSeconds());
        }
        if (request.burstCapacity() != null) {
            publishFieldChange(rule, "burstCapacity", String.valueOf(rule.getBurstCapacity()),
                    String.valueOf(request.burstCapacity()), requester);
            rule.setBurstCapacity(request.burstCapacity());
        }
        if (request.scope() != null) {
            publishFieldChange(rule, "scope", rule.getScope().name(), request.scope().name(), requester);
            rule.setScope(request.scope());
        }
        if (request.targetTier() != null) {
            publishFieldChange(rule, "targetTier",
                    rule.getTargetTier() != null ? String.valueOf(rule.getTargetTier()) : null,
                    String.valueOf(request.targetTier()), requester);
            rule.setTargetTier(request.targetTier());
        }
        if (request.targetUserId() != null) {
            publishFieldChange(rule, "targetUserId",
                    rule.getTargetUserId() != null ? rule.getTargetUserId().toString() : null,
                    request.targetUserId().toString(), requester);
            rule.setTargetUserId(request.targetUserId());
        }
        if (request.timeWindowStart() != null) {
            rule.setTimeWindowStart(request.timeWindowStart());
        }
        if (request.timeWindowEnd() != null) {
            rule.setTimeWindowEnd(request.timeWindowEnd());
        }
        if (request.priority() != null) {
            publishFieldChange(rule, "priority", String.valueOf(rule.getPriority()),
                    String.valueOf(request.priority()), requester);
            rule.setPriority(request.priority());
        }
        if (request.enabled() != null) {
            publishFieldChange(rule, "enabled", String.valueOf(rule.isEnabled()),
                    String.valueOf(request.enabled()), requester);
            rule.setEnabled(request.enabled());
        }

        RateLimitRule saved = ruleRepository.save(rule);
        evaluationService.evictRulesCache();
        evaluationService.invalidateBucketsByRule(saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public RateLimitRuleResponse toggleRule(UUID id, RateLimitToggleRequest request, UserPrincipal requester) {
        RateLimitRule rule = findRuleById(id);
        String oldValue = String.valueOf(rule.isEnabled());
        String newValue = String.valueOf(request.enabled());

        rule.setEnabled(request.enabled());
        RateLimitRule saved = ruleRepository.save(rule);

        publishAudit(saved.getId(), saved.getName(), AuditAction.TOGGLED, "enabled",
                oldValue, newValue, null, requester);
        evaluationService.evictRulesCache();
        evaluationService.invalidateBucketsByRule(saved.getId());

        if (saved.getPriority() >= 50) {
            notificationPublisher.publishToTier(CapabilityTier.ADMIN, NotificationEventBuilder.builder()
                    .titleKey("notification.rate_limit.toggled")
                    .bodyKey("notification.rate_limit.toggled.body")
                    .bodyArgs(saved.getName(), newValue)
                    .category(NotificationCategory.RATE_LIMIT)
                    .sourceModule("rate-limit")
                    .relatedEntityId(saved.getId())
                    .relatedEntityType("RATE_LIMIT_RULE")
                    .build());
        }

        return toResponse(saved);
    }

    @Transactional
    public void deleteRule(UUID id, UserPrincipal requester) {
        RateLimitRule rule = findRuleById(id);
        publishAudit(rule.getId(), rule.getName(), AuditAction.DELETED, null, null, null, null, requester);
        ruleRepository.delete(rule);
        evaluationService.evictRulesCache();
        evaluationService.invalidateBucketsByRule(rule.getId());
    }

    public RateLimitStatsResponse getStats() {
        return new RateLimitStatsResponse(
                evaluationService.getTotalViolations(),
                evaluationService.getActiveBuckets(),
                evaluationService.getViolationsByEndpoint()
        );
    }

    private RateLimitRule findRuleById(UUID id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RateLimitRule", id));
    }

    private void publishFieldChange(RateLimitRule rule, String fieldName, String oldValue,
                                     String newValue, UserPrincipal requester) {
        publishAudit(rule.getId(), rule.getName(), AuditAction.UPDATED, fieldName, oldValue, newValue, null, requester);
    }

    private void publishAudit(UUID ruleId, String ruleName, AuditAction action, String fieldName,
                               String oldValue, String newValue, String reason, UserPrincipal requester) {
        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(requester)
                .action(action)
                .entityType(AuditEntityType.RATE_LIMIT_RULE)
                .entityId(ruleId)
                .entityName(ruleName)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .details(reason)
                .sourceModule("rate-limit")
                .build());
    }

    private RateLimitRuleResponse toResponse(RateLimitRule rule) {
        return new RateLimitRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getDescription(),
                rule.getEndpointPattern(),
                rule.getHttpMethod(),
                rule.getLimitPerPeriod(),
                rule.getPeriodSeconds(),
                rule.getBurstCapacity(),
                rule.getScope(),
                rule.getTargetTier(),
                rule.getTargetUserId(),
                rule.getTimeWindowStart(),
                rule.getTimeWindowEnd(),
                rule.getPriority(),
                rule.isEnabled(),
                rule.getCreatedBy(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
