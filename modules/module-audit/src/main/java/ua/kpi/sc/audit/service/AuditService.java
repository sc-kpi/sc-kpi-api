package ua.kpi.sc.audit.service;

import java.io.PrintWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.audit.dto.AuditEventResponse;
import ua.kpi.sc.audit.dto.AuditStatsResponse;
import ua.kpi.sc.audit.entity.AuditEventEntity;
import ua.kpi.sc.audit.repository.AuditEventRepository;
import ua.kpi.sc.audit.repository.AuditEventSpecification;

/**
 * Query and export service for the centralized audit log.
 *
 * @since 0.4.0
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getAuditEvents(UUID actorId, String entityType, String action,
                                                    String sourceModule, UUID entityId,
                                                    Instant from, Instant to, String search,
                                                    Pageable pageable) {
        Specification<AuditEventEntity> spec = Specification.where(AuditEventSpecification.always());

        if (actorId != null) {
            spec = spec.and(AuditEventSpecification.hasActorId(actorId));
        }
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and(AuditEventSpecification.hasEntityType(entityType));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and(AuditEventSpecification.hasAction(action));
        }
        if (sourceModule != null && !sourceModule.isBlank()) {
            spec = spec.and(AuditEventSpecification.hasSourceModule(sourceModule));
        }
        if (entityId != null) {
            spec = spec.and(AuditEventSpecification.hasEntityId(entityId));
        }
        if (from != null) {
            spec = spec.and(AuditEventSpecification.createdAfter(from));
        }
        if (to != null) {
            spec = spec.and(AuditEventSpecification.createdBefore(to));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(AuditEventSpecification.searchByText(search));
        }

        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public void exportCsv(UUID actorId, String entityType, String action,
                          String sourceModule, UUID entityId,
                          Instant from, Instant to, String search,
                          PrintWriter writer) {
        Specification<AuditEventEntity> spec = Specification.where(AuditEventSpecification.always());

        if (actorId != null) {
            spec = spec.and(AuditEventSpecification.hasActorId(actorId));
        }
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and(AuditEventSpecification.hasEntityType(entityType));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and(AuditEventSpecification.hasAction(action));
        }
        if (sourceModule != null && !sourceModule.isBlank()) {
            spec = spec.and(AuditEventSpecification.hasSourceModule(sourceModule));
        }
        if (entityId != null) {
            spec = spec.and(AuditEventSpecification.hasEntityId(entityId));
        }
        if (from != null) {
            spec = spec.and(AuditEventSpecification.createdAfter(from));
        }
        if (to != null) {
            spec = spec.and(AuditEventSpecification.createdBefore(to));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(AuditEventSpecification.searchByText(search));
        }

        writer.println("Timestamp,Actor,Action,Entity Type,Entity,Field,Old Value,New Value,Details,Module,IP");

        repository.findAll(spec).forEach(e -> {
            writer.println(
                    escapeCsv(str(e.getCreatedAt())) + "," +
                    escapeCsv(str(e.getActorEmail())) + "," +
                    escapeCsv(str(e.getAction())) + "," +
                    escapeCsv(str(e.getEntityType())) + "," +
                    escapeCsv(str(e.getEntityName())) + "," +
                    escapeCsv(str(e.getFieldName())) + "," +
                    escapeCsv(str(e.getOldValue())) + "," +
                    escapeCsv(str(e.getNewValue())) + "," +
                    escapeCsv(str(e.getDetails())) + "," +
                    escapeCsv(str(e.getSourceModule())) + "," +
                    escapeCsv(str(e.getIpAddress()))
            );
        });

        writer.flush();
    }

    @Transactional(readOnly = true)
    public AuditStatsResponse getStats() {
        long total = repository.count();
        long last24h = repository.countByCreatedAtAfter(Instant.now().minus(24, ChronoUnit.HOURS));

        Map<String, Long> byEntityType = repository.findAll().stream()
                .collect(Collectors.groupingBy(AuditEventEntity::getEntityType, LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> byAction = repository.findAll().stream()
                .collect(Collectors.groupingBy(AuditEventEntity::getAction, LinkedHashMap::new, Collectors.counting()));

        return new AuditStatsResponse(total, last24h, byEntityType, byAction);
    }

    private AuditEventResponse toResponse(AuditEventEntity entity) {
        return new AuditEventResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getActorEmail(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getEntityName(),
                entity.getFieldName(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getDetails(),
                entity.getSourceModule(),
                entity.getIpAddress(),
                entity.getCreatedAt()
        );
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
