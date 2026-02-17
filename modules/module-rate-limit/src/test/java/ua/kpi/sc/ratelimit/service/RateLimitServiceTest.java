package ua.kpi.sc.ratelimit.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ua.kpi.sc.common.audit.AuditEvent;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.exception.ConflictException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.ratelimit.dto.CreateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.dto.RateLimitRuleResponse;
import ua.kpi.sc.ratelimit.dto.RateLimitToggleRequest;
import ua.kpi.sc.ratelimit.dto.UpdateRateLimitRuleRequest;
import ua.kpi.sc.ratelimit.entity.RateLimitRule;
import ua.kpi.sc.ratelimit.entity.RateLimitScope;
import ua.kpi.sc.ratelimit.repository.RateLimitRuleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimitRuleRepository ruleRepository;
    @Mock
    private RateLimitEvaluationService evaluationService;
    @Mock
    private AuditPublisher auditPublisher;
    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private RateLimitService service;

    private final UserPrincipal adminPrincipal = UserPrincipal.builder()
            .id(UUID.randomUUID())
            .email("admin@test.kpi.ua")
            .tier(CapabilityTier.ADMIN)
            .active(true)
            .build();

    @Test
    void createRule_success() {
        var request = new CreateRateLimitRuleRequest(
                "auth.login.ip", "Login limit", "/api/v1/auth/login", "POST",
                5, 60, 10, RateLimitScope.IP, null, null, null, null, 100, true);
        when(ruleRepository.existsByName("auth.login.ip")).thenReturn(false);

        RateLimitRule saved = buildRule("auth.login.ip");
        when(ruleRepository.save(any(RateLimitRule.class))).thenReturn(saved);

        RateLimitRuleResponse response = service.createRule(request, adminPrincipal);

        assertThat(response.name()).isEqualTo("auth.login.ip");
        verify(auditPublisher).publish(any(AuditEvent.class));
        verify(evaluationService).evictRulesCache();
    }

    @Test
    void createRule_duplicateNameThrowsConflict() {
        var request = new CreateRateLimitRuleRequest(
                "auth.login.ip", null, "/api/v1/auth/login", "POST",
                5, 60, 10, RateLimitScope.IP, null, null, null, null, 100, true);
        when(ruleRepository.existsByName("auth.login.ip")).thenReturn(true);

        assertThatThrownBy(() -> service.createRule(request, adminPrincipal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getRule_success() {
        UUID ruleId = UUID.randomUUID();
        RateLimitRule rule = buildRule("test.rule");
        rule.setId(ruleId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        RateLimitRuleResponse response = service.getRule(ruleId);

        assertThat(response.name()).isEqualTo("test.rule");
    }

    @Test
    void getRule_notFoundThrows() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRule(ruleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRule_updatesFields() {
        UUID ruleId = UUID.randomUUID();
        RateLimitRule rule = buildRule("test.rule");
        rule.setId(ruleId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(RateLimitRule.class))).thenReturn(rule);

        var request = new UpdateRateLimitRuleRequest(
                "Updated desc", null, null, 10, null, null,
                null, null, null, null, null, null, null);
        RateLimitRuleResponse response = service.updateRule(ruleId, request, adminPrincipal);

        assertThat(rule.getDescription()).isEqualTo("Updated desc");
        assertThat(rule.getLimitPerPeriod()).isEqualTo(10);
        verify(evaluationService).evictRulesCache();
        verify(evaluationService).invalidateBucketsByRule(ruleId);
    }

    @Test
    void toggleRule_success() {
        UUID ruleId = UUID.randomUUID();
        RateLimitRule rule = buildRule("test.rule");
        rule.setId(ruleId);
        rule.setEnabled(true);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(RateLimitRule.class))).thenReturn(rule);

        var request = new RateLimitToggleRequest(false);
        service.toggleRule(ruleId, request, adminPrincipal);

        assertThat(rule.isEnabled()).isFalse();
        verify(auditPublisher).publish(any(AuditEvent.class));
        verify(evaluationService).evictRulesCache();
    }

    @Test
    void deleteRule_success() {
        UUID ruleId = UUID.randomUUID();
        RateLimitRule rule = buildRule("test.rule");
        rule.setId(ruleId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        service.deleteRule(ruleId, adminPrincipal);

        verify(ruleRepository).delete(rule);
        verify(auditPublisher).publish(any(AuditEvent.class));
        verify(evaluationService).evictRulesCache();
    }

    @Test
    void deleteRule_notFoundThrows() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRule(ruleId, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listRules_returnsPaginatedResults() {
        var pageable = PageRequest.of(0, 20);
        RateLimitRule rule = buildRule("test.rule");
        when(ruleRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(rule)));

        Page<RateLimitRuleResponse> result = service.listRules(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("test.rule");
    }

    @Test
    void getStats_returnsStats() {
        when(evaluationService.getTotalViolations()).thenReturn(42L);
        when(evaluationService.getActiveBuckets()).thenReturn(100L);

        var stats = service.getStats();

        assertThat(stats.totalViolations()).isEqualTo(42L);
        assertThat(stats.activeBuckets()).isEqualTo(100L);
    }

    private RateLimitRule buildRule(String name) {
        return RateLimitRule.builder()
                .id(UUID.randomUUID())
                .name(name)
                .endpointPattern("/api/v1/**")
                .limitPerPeriod(60)
                .periodSeconds(60)
                .burstCapacity(90)
                .scope(RateLimitScope.IP)
                .priority(1)
                .enabled(true)
                .build();
    }
}
