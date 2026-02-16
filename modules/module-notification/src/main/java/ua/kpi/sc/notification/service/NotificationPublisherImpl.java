package ua.kpi.sc.notification.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ua.kpi.sc.common.notification.NotificationChannel;
import ua.kpi.sc.common.notification.NotificationEvent;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserQueryPort;
import ua.kpi.sc.notification.dto.NotificationResponse;
import ua.kpi.sc.notification.entity.NotificationEntity;
import ua.kpi.sc.notification.sse.SseEmitterRegistry;

/**
 * Implements the {@link NotificationPublisher} port. Creates in-app notifications
 * and triggers email delivery asynchronously. Never throws exceptions to callers.
 *
 * @since 0.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisherImpl implements NotificationPublisher {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationEmailService emailService;
    private final SseEmitterRegistry sseRegistry;
    private final UserQueryPort userQueryPort;
    private final PlatformTransactionManager transactionManager;

    @Async
    @Override
    public void publishToUser(UUID userId, NotificationEvent event) {
        try {
            deliverToUser(userId, event);
        } catch (Exception e) {
            log.error("Failed to publish notification to user {}: titleKey={}", userId, event.titleKey(), e);
        }
    }

    @Async
    @Override
    public void publishToTier(CapabilityTier tier, NotificationEvent event) {
        try {
            List<UUID> userIds = userQueryPort.findActiveUserIdsByMinimumTier(tier);
            for (UUID userId : userIds) {
                try {
                    deliverToUser(userId, event);
                } catch (Exception e) {
                    log.error("Failed to publish notification to user {} (tier broadcast): titleKey={}",
                            userId, event.titleKey(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish tier broadcast: tier={}, titleKey={}", tier, event.titleKey(), e);
        }
    }

    @Async
    @Override
    public void publishToAll(NotificationEvent event) {
        try {
            List<UUID> userIds = userQueryPort.findAllActiveUserIds();
            for (UUID userId : userIds) {
                try {
                    deliverToUser(userId, event);
                } catch (Exception e) {
                    log.error("Failed to publish notification to user {} (broadcast): titleKey={}",
                            userId, event.titleKey(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to publish broadcast: titleKey={}", event.titleKey(), e);
        }
    }

    private void deliverToUser(UUID userId, NotificationEvent event) {
        // Check IN_APP preference and create notification
        if (preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.IN_APP)) {
            var tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            NotificationEntity saved = tx.execute(status -> notificationService.createNotification(
                    userId, event.titleKey(), event.bodyKey(), event.bodyArgs(),
                    event.category().name(), event.sourceModule(),
                    event.relatedEntityId(), event.relatedEntityType()
            ));

            if (saved != null) {
                // Push via SSE
                sseRegistry.sendToUser(userId, toResponse(saved));
            }
        }

        // Check EMAIL preference and send email
        if (preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.EMAIL)) {
            String email = userQueryPort.getEmailById(userId);
            if (email != null) {
                emailService.sendNotificationEmail(email, event);
            }
        }
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
