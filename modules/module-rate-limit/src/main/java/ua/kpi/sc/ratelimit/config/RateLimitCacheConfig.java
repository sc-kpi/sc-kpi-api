package ua.kpi.sc.ratelimit.config;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitCacheConfig {

    @Bean
    public CacheManager rateLimitCacheManager(RateLimitProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("rateLimitRules");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.getRulesCacheTtlSeconds(), TimeUnit.SECONDS)
                .maximumSize(500));
        return cacheManager;
    }
}
