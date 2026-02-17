package ua.kpi.sc.ratelimit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import ua.kpi.sc.common.featureflag.FeatureFlagChecker;
import ua.kpi.sc.ratelimit.filter.RateLimitFilter;
import ua.kpi.sc.ratelimit.service.RateLimitEvaluationService;
import ua.kpi.sc.ratelimit.store.BucketStore;
import ua.kpi.sc.ratelimit.store.CaffeineBucketStore;

@Configuration
public class RateLimitFilterRegistration {

    @Bean
    public BucketStore bucketStore(RateLimitProperties properties) {
        return new CaffeineBucketStore(properties);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitProperties properties,
                                           RateLimitEvaluationService evaluationService,
                                           FeatureFlagChecker featureFlagChecker,
                                           ObjectMapper objectMapper) {
        return new RateLimitFilter(properties, evaluationService, featureFlagChecker, objectMapper);
    }
}
