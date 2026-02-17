package ua.kpi.sc.featureflag.config;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
@EnableConfigurationProperties(FeatureFlagProperties.class)
public class FeatureFlagCacheConfig {

    @Bean
    @Primary
    public CacheManager featureFlagCacheManager(FeatureFlagProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("featureFlags");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(500));
        return cacheManager;
    }
}
