package ua.kpi.sc.notification.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.notification.NotificationEventBuilder;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.notification.dto.BroadcastRequest;
import ua.kpi.sc.notification.dto.NotificationResponse;
import ua.kpi.sc.notification.dto.NotificationStatsResponse;
import ua.kpi.sc.notification.entity.NotificationEntity;
import ua.kpi.sc.notification.repository.NotificationRepository;
import ua.kpi.sc.notification.repository.NotificationSpecification;

/**
 * Admin-level notification management service.
 *
 * @since 0.5.0
 */
@Service
@RequiredArgsConstructor
public class NotificationAdminService {

    private final NotificationRepository notificationRepository;
    private final NotificationPublisher notificationPublisher;
    private final NotificationService notificationService;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllNotifications(String category, String search,
                                                          Instant from, Instant to,
                                                          Pageable pageable) {
        Specification<NotificationEntity> spec = Specification.where(NotificationSpecification.always());

        if (category != null && !category.isBlank()) {
            spec = spec.and(NotificationSpecification.hasCategory(category));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(NotificationSpecification.searchByText(search));
        }
        if (from != null) {
            spec = spec.and(NotificationSpecification.createdAfter(from));
        }
        if (to != null) {
            spec = spec.and(NotificationSpecification.createdBefore(to));
        }

        return notificationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public void broadcast(BroadcastRequest request, UserPrincipal requester) {
        var event = NotificationEventBuilder.builder()
                .titleKey(request.titleKey())
                .bodyKey(request.bodyKey())
                .bodyArgs(request.bodyArgs())
                .category(request.category())
                .sourceModule("notification")
                .build();

        if (request.targetTier() != null && !request.targetTier().isBlank()) {
            CapabilityTier tier = CapabilityTier.valueOf(request.targetTier());
            notificationPublisher.publishToTier(tier, event);
        } else {
            notificationPublisher.publishToAll(event);
        }

        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(requester)
                .action(AuditAction.NOTIFICATION_BROADCAST)
                .entityType(AuditEntityType.NOTIFICATION)
                .sourceModule("notification")
                .details("Broadcast: " + request.titleKey() +
                        (request.targetTier() != null ? " to tier " + request.targetTier() : " to all"))
                .build());
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getStats() {
        long total = notificationRepository.count();
        long last24h = notificationRepository.countByCreatedAtAfter(
                Instant.now().minus(24, ChronoUnit.HOURS));

        Map<String, Long> byCategory = notificationRepository.findAll().stream()
                .collect(Collectors.groupingBy(NotificationEntity::getCategory,
                        LinkedHashMap::new, Collectors.counting()));

        return new NotificationStatsResponse(total, last24h, byCategory);
    }

    @Transactional
    public int cleanup(int days, UserPrincipal requester) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        int deleted = notificationService.deleteOldNotifications(cutoff);

        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(requester)
                .action(AuditAction.NOTIFICATION_CLEANUP)
                .entityType(AuditEntityType.NOTIFICATION)
                .sourceModule("notification")
                .details("Cleaned up " + deleted + " notifications older than " + days + " days")
                .build());

        return deleted;
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(), entity.getUserId(), entity.getTitleKey(), entity.getBodyKey(),
                entity.getBodyArgs(), entity.getCategory(), entity.getSourceModule(),
                entity.getRelatedEntityId(), entity.getRelatedEntityType(),
                entity.isRead(), entity.getReadAt(), entity.getCreatedAt()
        );
    }
}
