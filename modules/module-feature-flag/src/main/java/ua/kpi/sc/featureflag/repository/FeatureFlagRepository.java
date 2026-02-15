package ua.kpi.sc.featureflag.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.kpi.sc.featureflag.entity.FeatureFlag;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    Optional<FeatureFlag> findByKey(String key);

    boolean existsByKey(String key);

    List<FeatureFlag> findAllByEnabledTrue();

    @Query("SELECT DISTINCT f FROM FeatureFlag f LEFT JOIN FETCH f.overrides")
    List<FeatureFlag> findAllWithOverrides();

    @Query("SELECT f FROM FeatureFlag f LEFT JOIN FETCH f.overrides WHERE f.key = :key")
    Optional<FeatureFlag> findByKeyWithOverrides(@Param("key") String key);
}
