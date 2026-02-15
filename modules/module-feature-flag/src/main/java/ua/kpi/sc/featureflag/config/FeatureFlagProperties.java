package ua.kpi.sc.featureflag.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "feature-flags")
public class FeatureFlagProperties {

    private Map<String, Boolean> overrides = new HashMap<>();

    private Map<String, Boolean> defaults = new HashMap<>();

    private int cacheTtlSeconds = 30;
}
