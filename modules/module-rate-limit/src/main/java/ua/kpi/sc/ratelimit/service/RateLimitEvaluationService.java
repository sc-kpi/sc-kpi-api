package ua.kpi.sc.ratelimit.service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import ua.kpi.sc.ratelimit.config.RateLimitProperties;
import ua.kpi.sc.ratelimit.entity.RateLimitRule;
import ua.kpi.sc.ratelimit.entity.RateLimitScope;
import ua.kpi.sc.ratelimit.repository.RateLimitRuleRepository;
import ua.kpi.sc.ratelimit.store.BucketStore;

@Slf4j
@Service
public class RateLimitEvaluationService {

    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final Comparator<RateLimitRule> RULE_COMPARATOR = Comparator
            .comparingInt(RateLimitRule::getPriority).reversed()
            .thenComparing(r -> scopeSpecificity(r.getScope()));

    private final RateLimitRuleRepository ruleRepository;
    private final BucketStore bucketStore;
    private final RateLimitProperties properties;

    private final AtomicLong totalViolations = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> violationsByEndpoint = new ConcurrentHashMap<>();

    public RateLimitEvaluationService(RateLimitRuleRepository ruleRepository,
                                      BucketStore bucketStore,
                                      RateLimitProperties properties) {
        this.ruleRepository = ruleRepository;
        this.bucketStore = bucketStore;
        this.properties = properties;
    }

    @Cacheable(cacheManager = "rateLimitCacheManager", cacheNames = "rateLimitRules", key = "'all'")
    public List<RateLimitRule> loadEnabledRules() {
        return ruleRepository.findByEnabledTrue();
    }

    @CacheEvict(cacheManager = "rateLimitCacheManager", cacheNames = "rateLimitRules", allEntries = true)
    public void evictRulesCache() {
        log.debug("Rate limit rules cache evicted");
    }

    public RateLimitRule resolveRule(String path, String method, String clientIp,
                                     UUID userId, Integer tierLevel) {
        List<RateLimitRule> rules = loadEnabledRules();
        LocalTime now = ZonedDateTime.now(KYIV_ZONE).toLocalTime();

        return rules.stream()
                .filter(rule -> matchesEndpoint(rule, path, method))
                .filter(rule -> matchesScope(rule, clientIp, userId, tierLevel))
                .filter(rule -> matchesTimeWindow(rule, now))
                .min(RULE_COMPARATOR)
                .orElse(null);
    }

    public ConsumptionProbe tryConsume(RateLimitRule rule, String clientIp,
                                       UUID userId, Integer tierLevel) {
        String bucketKey = buildBucketKey(rule, clientIp, userId, tierLevel);
        Supplier<Bucket> bucketSupplier = () -> buildBucket(rule);
        Bucket bucket = bucketStore.resolveBucket(bucketKey, bucketSupplier);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public void recordViolation(String endpointPattern) {
        if (!properties.isTrackViolations()) {
            return;
        }
        totalViolations.incrementAndGet();
        violationsByEndpoint
                .computeIfAbsent(endpointPattern, k -> new AtomicLong())
                .incrementAndGet();

        // Trim if too many entries
        if (violationsByEndpoint.size() > properties.getMaxViolationEntries()) {
            violationsByEndpoint.clear();
        }
    }

    public long getTotalViolations() {
        return totalViolations.get();
    }

    public Map<String, Long> getViolationsByEndpoint() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        violationsByEndpoint.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public void invalidateBuckets() {
        bucketStore.invalidateAll();
    }

    public void invalidateBucketsByRule(UUID ruleId) {
        bucketStore.invalidateByPrefix(ruleId.toString());
    }

    public long getActiveBuckets() {
        return bucketStore.estimatedSize();
    }

    String buildBucketKey(RateLimitRule rule, String clientIp,
                          UUID userId, Integer tierLevel) {
        String ruleId = rule.getId().toString();
        return switch (rule.getScope()) {
            case GLOBAL -> "global:" + ruleId;
            case IP -> "ip:" + ruleId + ":" + clientIp;
            case USER -> "user:" + ruleId + ":" + (userId != null ? userId : "anonymous");
            case TIER -> "tier:" + ruleId + ":" + (tierLevel != null ? tierLevel : 0);
        };
    }

    private Bucket buildBucket(RateLimitRule rule) {
        Bandwidth bandwidth = Bandwidth.classic(
                rule.getBurstCapacity(),
                Refill.greedy(rule.getLimitPerPeriod(), Duration.ofSeconds(rule.getPeriodSeconds()))
        );
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private boolean matchesEndpoint(RateLimitRule rule, String path, String method) {
        if (!PATH_MATCHER.match(rule.getEndpointPattern(), path)) {
            return false;
        }
        return rule.getHttpMethod() == null || rule.getHttpMethod().equalsIgnoreCase(method);
    }

    private boolean matchesScope(RateLimitRule rule, String clientIp,
                                  UUID userId, Integer tierLevel) {
        return switch (rule.getScope()) {
            case GLOBAL -> true;
            case IP -> clientIp != null;
            case USER -> userId != null;
            case TIER -> {
                if (rule.getTargetTier() != null) {
                    yield tierLevel != null && rule.getTargetTier().equals(tierLevel);
                }
                yield tierLevel != null;
            }
        };
    }

    private boolean matchesTimeWindow(RateLimitRule rule, LocalTime now) {
        if (rule.getTimeWindowStart() == null || rule.getTimeWindowEnd() == null) {
            return true;
        }
        if (rule.getTimeWindowStart().isBefore(rule.getTimeWindowEnd())) {
            return !now.isBefore(rule.getTimeWindowStart()) && now.isBefore(rule.getTimeWindowEnd());
        }
        // Wraps midnight (e.g., 22:00–06:00)
        return !now.isBefore(rule.getTimeWindowStart()) || now.isBefore(rule.getTimeWindowEnd());
    }

    private static int scopeSpecificity(RateLimitScope scope) {
        return switch (scope) {
            case USER -> 0;
            case TIER -> 1;
            case IP -> 2;
            case GLOBAL -> 3;
        };
    }
}
