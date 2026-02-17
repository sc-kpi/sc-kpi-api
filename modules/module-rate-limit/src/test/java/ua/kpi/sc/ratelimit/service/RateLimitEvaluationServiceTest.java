package ua.kpi.sc.ratelimit.service;

import java.util.List;
import java.util.UUID;

import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.sc.ratelimit.config.RateLimitProperties;
import ua.kpi.sc.ratelimit.entity.RateLimitRule;
import ua.kpi.sc.ratelimit.entity.RateLimitScope;
import ua.kpi.sc.ratelimit.repository.RateLimitRuleRepository;
import ua.kpi.sc.ratelimit.store.BucketStore;
import ua.kpi.sc.ratelimit.store.CaffeineBucketStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitEvaluationServiceTest {

    @Mock
    private RateLimitRuleRepository ruleRepository;

    private BucketStore bucketStore;
    private RateLimitProperties properties;
    private RateLimitEvaluationService service;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setMaxBucketCount(1000);
        properties.setBucketTtlSeconds(60);
        properties.setTrackViolations(true);
        properties.setMaxViolationEntries(100);
        bucketStore = new CaffeineBucketStore(properties);
        service = new RateLimitEvaluationService(ruleRepository, bucketStore, properties);
    }

    @Test
    void resolveRule_matchesExactEndpoint() {
        RateLimitRule loginRule = buildRule("auth.login.ip", "/api/v1/auth/login", "POST",
                RateLimitScope.IP, 100, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(loginRule));

        RateLimitRule resolved = service.resolveRule("/api/v1/auth/login", "POST",
                "192.168.1.1", null, null);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo("auth.login.ip");
    }

    @Test
    void resolveRule_matchesWildcardPattern() {
        RateLimitRule globalRule = buildRule("api.global.ip", "/api/v1/**", null,
                RateLimitScope.IP, 1, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(globalRule));

        RateLimitRule resolved = service.resolveRule("/api/v1/users", "GET",
                "192.168.1.1", null, null);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo("api.global.ip");
    }

    @Test
    void resolveRule_higherPriorityWins() {
        RateLimitRule lowPriority = buildRule("api.global.ip", "/api/v1/**", null,
                RateLimitScope.IP, 1, null, null);
        RateLimitRule highPriority = buildRule("auth.login.ip", "/api/v1/auth/login", "POST",
                RateLimitScope.IP, 100, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(lowPriority, highPriority));

        RateLimitRule resolved = service.resolveRule("/api/v1/auth/login", "POST",
                "192.168.1.1", null, null);

        assertThat(resolved.getName()).isEqualTo("auth.login.ip");
    }

    @Test
    void resolveRule_userScopeRequiresUserId() {
        RateLimitRule userRule = buildRule("api.user", "/api/v1/**", null,
                RateLimitScope.USER, 1, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(userRule));

        RateLimitRule resolved = service.resolveRule("/api/v1/users", "GET",
                "192.168.1.1", null, null);

        assertThat(resolved).isNull();
    }

    @Test
    void resolveRule_userScopeMatchesWithUserId() {
        RateLimitRule userRule = buildRule("api.user", "/api/v1/**", null,
                RateLimitScope.USER, 1, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(userRule));

        RateLimitRule resolved = service.resolveRule("/api/v1/users", "GET",
                "192.168.1.1", UUID.randomUUID(), null);

        assertThat(resolved).isNotNull();
    }

    @Test
    void resolveRule_noMatchReturnsNull() {
        RateLimitRule loginRule = buildRule("auth.login.ip", "/api/v1/auth/login", "POST",
                RateLimitScope.IP, 100, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(loginRule));

        RateLimitRule resolved = service.resolveRule("/api/v1/users", "GET",
                "192.168.1.1", null, null);

        assertThat(resolved).isNull();
    }

    @Test
    void resolveRule_httpMethodMismatchExcludesRule() {
        RateLimitRule postOnly = buildRule("auth.login.ip", "/api/v1/auth/login", "POST",
                RateLimitScope.IP, 100, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(postOnly));

        RateLimitRule resolved = service.resolveRule("/api/v1/auth/login", "GET",
                "192.168.1.1", null, null);

        assertThat(resolved).isNull();
    }

    @Test
    void resolveRule_tierTargetFiltersCorrectly() {
        RateLimitRule tierRule = buildRule("api.tier.basic", "/api/v1/**", null,
                RateLimitScope.TIER, 1, 1, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(tierRule));

        // tier 1 = BASIC matches
        RateLimitRule resolved = service.resolveRule("/api/v1/users", "GET",
                "192.168.1.1", UUID.randomUUID(), 1);
        assertThat(resolved).isNotNull();

        // tier 2 does not match targetTier=1
        resolved = service.resolveRule("/api/v1/users", "GET",
                "192.168.1.1", UUID.randomUUID(), 2);
        assertThat(resolved).isNull();
    }

    @Test
    void tryConsume_allowsWithinLimit() {
        RateLimitRule rule = buildRule("test.rule", "/api/**", null,
                RateLimitScope.IP, 1, null, null);
        rule.setLimitPerPeriod(10);
        rule.setBurstCapacity(10);
        rule.setPeriodSeconds(60);

        ConsumptionProbe probe = service.tryConsume(rule, "192.168.1.1", null, null);

        assertThat(probe.isConsumed()).isTrue();
        assertThat(probe.getRemainingTokens()).isEqualTo(9);
    }

    @Test
    void tryConsume_rejectsWhenExhausted() {
        RateLimitRule rule = buildRule("test.rule", "/api/**", null,
                RateLimitScope.IP, 1, null, null);
        rule.setLimitPerPeriod(1);
        rule.setBurstCapacity(1);
        rule.setPeriodSeconds(60);

        service.tryConsume(rule, "192.168.1.1", null, null);
        ConsumptionProbe probe = service.tryConsume(rule, "192.168.1.1", null, null);

        assertThat(probe.isConsumed()).isFalse();
    }

    @Test
    void recordViolation_tracksCorrectly() {
        service.recordViolation("/api/v1/auth/login");
        service.recordViolation("/api/v1/auth/login");
        service.recordViolation("/api/v1/users");

        assertThat(service.getTotalViolations()).isEqualTo(3);
        assertThat(service.getViolationsByEndpoint()).containsEntry("/api/v1/auth/login", 2L);
        assertThat(service.getViolationsByEndpoint()).containsEntry("/api/v1/users", 1L);
    }

    @Test
    void buildBucketKey_globalScope() {
        RateLimitRule rule = buildRule("test", "/api/**", null, RateLimitScope.GLOBAL, 1, null, null);

        String key = service.buildBucketKey(rule, "1.2.3.4", null, null);

        assertThat(key).startsWith("global:" + rule.getId());
    }

    @Test
    void buildBucketKey_ipScope() {
        RateLimitRule rule = buildRule("test", "/api/**", null, RateLimitScope.IP, 1, null, null);

        String key = service.buildBucketKey(rule, "1.2.3.4", null, null);

        assertThat(key).contains("ip:").contains("1.2.3.4");
    }

    @Test
    void buildBucketKey_userScope() {
        RateLimitRule rule = buildRule("test", "/api/**", null, RateLimitScope.USER, 1, null, null);
        UUID userId = UUID.randomUUID();

        String key = service.buildBucketKey(rule, "1.2.3.4", userId, null);

        assertThat(key).contains("user:").contains(userId.toString());
    }

    @Test
    void buildBucketKey_tierScope() {
        RateLimitRule rule = buildRule("test", "/api/**", null, RateLimitScope.TIER, 1, null, null);

        String key = service.buildBucketKey(rule, "1.2.3.4", null, 3);

        assertThat(key).contains("tier:").contains("3");
    }

    @Test
    void resolveRule_tierScopeWithoutTargetTierMatchesAnyTier() {
        // targetTier is null => matches any user with a tier
        RateLimitRule tierRule = buildRule("api.tier.any", "/api/v1/**", null,
                RateLimitScope.TIER, 1, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(tierRule));

        RateLimitRule resolved = service.resolveRule("/api/v1/users", "GET",
                "192.168.1.1", UUID.randomUUID(), 3);
        assertThat(resolved).isNotNull();
    }

    @Test
    void resolveRule_globalScopeMatchesAlways() {
        RateLimitRule globalRule = buildRule("api.global", "/api/v1/**", null,
                RateLimitScope.GLOBAL, 1, null, null);
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(globalRule));

        RateLimitRule resolved = service.resolveRule("/api/v1/users", "GET",
                null, null, null);
        assertThat(resolved).isNotNull();
    }

    @Test
    void recordViolation_disabledTracking_doesNotCount() {
        properties.setTrackViolations(false);
        service.recordViolation("/api/v1/auth/login");
        assertThat(service.getTotalViolations()).isEqualTo(0);
    }

    @Test
    void invalidateBuckets_clearsAll() {
        RateLimitRule rule = buildRule("test", "/api/**", null, RateLimitScope.IP, 1, null, null);
        rule.setLimitPerPeriod(100);
        rule.setBurstCapacity(100);
        rule.setPeriodSeconds(60);
        service.tryConsume(rule, "1.2.3.4", null, null);
        assertThat(service.getActiveBuckets()).isGreaterThan(0);

        service.invalidateBuckets();
        assertThat(service.getActiveBuckets()).isEqualTo(0);
    }

    @Test
    void invalidateBucketsByRule_callsStore() {
        UUID ruleId = UUID.randomUUID();
        // Just verify it delegates without error
        service.invalidateBucketsByRule(ruleId);
        // No exception means success
    }

    @Test
    void buildBucketKey_userScope_nullUserId() {
        RateLimitRule rule = buildRule("test", "/api/**", null, RateLimitScope.USER, 1, null, null);

        String key = service.buildBucketKey(rule, "1.2.3.4", null, null);

        assertThat(key).contains("user:").contains("anonymous");
    }

    @Test
    void buildBucketKey_tierScope_nullTier() {
        RateLimitRule rule = buildRule("test", "/api/**", null, RateLimitScope.TIER, 1, null, null);

        String key = service.buildBucketKey(rule, "1.2.3.4", null, null);

        assertThat(key).contains("tier:").contains("0");
    }

    private RateLimitRule buildRule(String name, String pattern, String method,
                                    RateLimitScope scope, int priority,
                                    Integer targetTier, UUID targetUserId) {
        return RateLimitRule.builder()
                .id(UUID.randomUUID())
                .name(name)
                .endpointPattern(pattern)
                .httpMethod(method)
                .limitPerPeriod(60)
                .periodSeconds(60)
                .burstCapacity(90)
                .scope(scope)
                .targetTier(targetTier)
                .targetUserId(targetUserId)
                .priority(priority)
                .enabled(true)
                .build();
    }
}
