package ua.kpi.sc.featureflag.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.featureflag.config.FeatureFlagProperties;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.featureflag.entity.FeatureFlag;
import ua.kpi.sc.featureflag.entity.FeatureFlagOverride;
import ua.kpi.sc.featureflag.entity.OverrideType;
import ua.kpi.sc.featureflag.repository.FeatureFlagRepository;

@Service
@RequiredArgsConstructor
public class FeatureFlagEvaluationService {

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagProperties properties;

    @Cacheable(cacheManager = "featureFlagCacheManager", cacheNames = "featureFlags",
            key = "#key + ':' + #userId + ':' + #tierLevel")
    @Transactional(readOnly = true)
    public boolean evaluate(String key, UUID userId, Integer tierLevel) {
        // 1. Config file override (highest priority)
        Boolean configOverride = properties.getOverrides().get(key);
        if (configOverride != null) {
            return configOverride;
        }

        // 2-5. DB-based evaluation
        var flagOpt = flagRepository.findByKeyWithOverrides(key);
        if (flagOpt.isEmpty()) {
            // 6. Config file default (lowest priority)
            Boolean configDefault = properties.getDefaults().get(key);
            if (configDefault != null) {
                return configDefault;
            }
            throw new ResourceNotFoundException("FeatureFlag", key);
        }

        return evaluateFlag(flagOpt.get(), userId, tierLevel);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> evaluateAll(UUID userId, Integer tierLevel) {
        Map<String, Boolean> result = new HashMap<>();

        // Start with config defaults
        result.putAll(properties.getDefaults());

        // Evaluate all DB flags directly from loaded entities (no re-query)
        var flags = flagRepository.findAllWithOverrides();
        for (FeatureFlag flag : flags) {
            result.put(flag.getKey(), evaluateFlag(flag, userId, tierLevel));
        }

        // Apply config overrides (highest priority)
        result.putAll(properties.getOverrides());

        return result;
    }

    private boolean evaluateFlag(FeatureFlag flag, UUID userId, Integer tierLevel) {
        // Per-user override
        if (userId != null) {
            for (FeatureFlagOverride override : flag.getOverrides()) {
                if (override.getOverrideType() == OverrideType.USER
                        && userId.equals(override.getUserId())) {
                    return override.isEnabled();
                }
            }
        }

        // Per-tier override
        if (tierLevel != null) {
            for (FeatureFlagOverride override : flag.getOverrides()) {
                if (override.getOverrideType() == OverrideType.TIER
                        && tierLevel.equals(override.getTierLevel())) {
                    return override.isEnabled();
                }
            }
        }

        // Percentage rollout
        if (userId != null && flag.getRolloutPercentage() < 100 && flag.isEnabled()) {
            int hash = Math.abs((userId + ":" + flag.getKey()).hashCode()) % 100;
            return hash < flag.getRolloutPercentage();
        }

        // Global DB default
        return flag.isEnabled();
    }

    @CacheEvict(cacheManager = "featureFlagCacheManager", cacheNames = "featureFlags", allEntries = true)
    public void evictCache() {
        // Cache evicted by annotation
    }
}
