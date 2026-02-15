package ua.kpi.sc.common.featureflag;

import java.util.UUID;

/**
 * Port interface for feature flag evaluation. Allows any module to check
 * feature flags without depending on module-feature-flag directly.
 *
 * @since 0.3.0
 */
public interface FeatureFlagChecker {

    /**
     * Evaluates whether a feature flag is enabled for the given context.
     *
     * @param key       the feature flag key
     * @param userId    the user ID (nullable for unauthenticated requests)
     * @param tierLevel the user's capability tier level (nullable)
     * @return {@code true} if the flag is enabled
     */
    boolean isEnabled(String key, UUID userId, Integer tierLevel);
}
