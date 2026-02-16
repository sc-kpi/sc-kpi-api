package ua.kpi.sc.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.sc.common.exception.ForbiddenException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.notification.dto.NotificationResponse;
import ua.kpi.sc.notification.entity.NotificationEntity;
import ua.kpi.sc.notification.repository.NotificationRepository;
import ua.kpi.sc.notification.repository.NotificationSpecification;

/**
 * Service for user-facing notification operations.
 *
 * @since 0.5.0
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationEntity createNotification(UUID userId, String titleKey, String bodyKey,
                                                  String[] bodyArgs, String category, String sourceModule,
                                                  UUID relatedEntityId, String relatedEntityType) {
        var entity = NotificationEntity.builder()
                .userId(userId)
                .titleKey(titleKey)
                .bodyKey(bodyKey)
                .bodyArgs(bodyArgs)
                .category(category)
                .sourceModule(sourceModule)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();
        return notificationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, String category,
                                                            Boolean isRead, Instant from, Instant to,
                                                            Pageable pageable) {
        Specification<NotificationEntity> spec = Specification.where(NotificationSpecification.hasUserId(userId));

        if (category != null && !category.isBlank()) {
            spec = spec.and(NotificationSpecification.hasCategory(category));
        }
        if (isRead != null) {
            spec = spec.and(NotificationSpecification.isRead(isRead));
        }
        if (from != null) {
            spec = spec.and(NotificationSpecification.createdAfter(from));
        }
        if (to != null) {
            spec = spec.and(NotificationSpecification.createdBefore(to));
        }

        return notificationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Cannot mark another user's notification as read");
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markBatchAsRead(UUID userId, List<UUID> ids) {
        for (UUID id : ids) {
            markAsRead(userId, id);
        }
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllReadByUserId(userId, Instant.now());
    }

    @Transactional
    public int deleteOldNotifications(Instant cutoff) {
        return notificationRepository.deleteOlderThan(cutoff);
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
