package ua.kpi.sc.featureflag.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.sc.featureflag.entity.FeatureFlagAuditLog;

public interface FeatureFlagAuditLogRepository extends JpaRepository<FeatureFlagAuditLog, UUID> {

    Page<FeatureFlagAuditLog> findByFlagIdOrderByChangedAtDesc(UUID flagId, Pageable pageable);

    Page<FeatureFlagAuditLog> findAllByOrderByChangedAtDesc(Pageable pageable);
}
