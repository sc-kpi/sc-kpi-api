package ua.kpi.sc.audit.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ua.kpi.sc.audit.entity.AuditEventEntity;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID>,
        JpaSpecificationExecutor<AuditEventEntity> {

    long countByCreatedAtAfter(Instant since);

    @Query("SELECT MAX(e.createdAt) FROM AuditEventEntity e")
    Instant findLatestEventTimestamp();
}
