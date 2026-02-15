package ua.kpi.sc.featureflag.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.featureflag.entity.FeatureFlagOverride;
import ua.kpi.sc.featureflag.entity.OverrideType;

public interface FeatureFlagOverrideRepository extends JpaRepository<FeatureFlagOverride, UUID> {

    Optional<FeatureFlagOverride> findByFlagIdAndOverrideTypeAndTierLevel(
            UUID flagId, OverrideType overrideType, Integer tierLevel);

    Optional<FeatureFlagOverride> findByFlagIdAndOverrideTypeAndUserId(
            UUID flagId, OverrideType overrideType, UUID userId);

    void deleteByFlagIdAndOverrideTypeAndTierLevel(UUID flagId, OverrideType overrideType, Integer tierLevel);

    void deleteByFlagIdAndOverrideTypeAndUserId(UUID flagId, OverrideType overrideType, UUID userId);
}
