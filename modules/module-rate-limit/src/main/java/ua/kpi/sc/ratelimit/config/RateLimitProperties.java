package ua.kpi.sc.ratelimit.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private String storageType = "in-memory";

    private int maxBucketCount = 100_000;

    private int bucketTtlSeconds = 3600;

    private int rulesCacheTtlSeconds = 30;

    private boolean trackViolations = true;

    private int maxViolationEntries = 10_000;

    private List<String> whitelistedIps = new ArrayList<>();

    private List<String> excludedPatterns = new ArrayList<>();

    private Map<Integer, Integer> tierDefaults = new HashMap<>();
}
