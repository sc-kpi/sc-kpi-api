package ua.kpi.sc.featureflag.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagCacheConfigTest {

    private final FeatureFlagCacheConfig cacheConfig = new FeatureFlagCacheConfig();

    @Test
    void cacheManager_createsCorrectCacheWithTtl() {
        var properties = new FeatureFlagProperties();
        properties.setCacheTtlSeconds(60);

        CacheManager cacheManager = cacheConfig.featureFlagCacheManager(properties);

        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
        assertThat(cacheManager.getCache("featureFlags")).isNotNull();
    }

    @Test
    void cacheManager_cacheNameIsFeatureFlags() {
        var properties = new FeatureFlagProperties();

        CacheManager cacheManager = cacheConfig.featureFlagCacheManager(properties);

        assertThat(cacheManager.getCacheNames()).contains("featureFlags");
    }
}
