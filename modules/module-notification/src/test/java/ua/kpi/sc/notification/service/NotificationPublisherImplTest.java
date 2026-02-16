package ua.kpi.sc.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationChannel;
import ua.kpi.sc.common.notification.NotificationEvent;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserQueryPort;
import ua.kpi.sc.notification.entity.NotificationEntity;
import ua.kpi.sc.notification.sse.SseEmitterRegistry;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherImplTest {

    @Mock private NotificationService notificationService;
    @Mock private NotificationPreferenceService preferenceService;
    @Mock private NotificationEmailService emailService;
    @Mock private SseEmitterRegistry sseRegistry;
    @Mock private UserQueryPort userQueryPort;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private NotificationPublisherImpl publisher;

    private NotificationEvent createEvent() {
        return new NotificationEvent("t", "b", null, NotificationCategory.SYSTEM,
                "notification", null, null, null);
    }

    private NotificationEntity createSavedEntity(UUID userId) {
        return NotificationEntity.builder()
                .id(UUID.randomUUID()).userId(userId)
                .titleKey("t").bodyKey("b").category("SYSTEM").sourceModule("notification")
                .createdAt(Instant.now()).build();
    }

    @Nested
    class PublishToUser {

        @Test
        void deliversInAppAndEmail_whenBothEnabled() {
            UUID userId = UUID.randomUUID();
            var event = createEvent();
            var savedEntity = createSavedEntity(userId);

            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.IN_APP))
                    .thenReturn(true);
            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.EMAIL))
                    .thenReturn(true);
            when(transactionManager.getTransaction(any()))
                    .thenReturn(new SimpleTransactionStatus());
            when(notificationService.createNotification(
                    eq(userId), eq("t"), eq("b"), any(), eq("SYSTEM"), eq("notification"), any(), any()))
                    .thenReturn(savedEntity);
            when(userQueryPort.getEmailById(userId)).thenReturn("user@kpi.ua");

            publisher.publishToUser(userId, event);

            verify(sseRegistry).sendToUser(eq(userId), any());
            verify(emailService).sendNotificationEmail(eq("user@kpi.ua"), eq(event));
        }

        @Test
        void skipsInApp_whenDisabled() {
            UUID userId = UUID.randomUUID();
            var event = createEvent();

            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.IN_APP))
                    .thenReturn(false);
            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.EMAIL))
                    .thenReturn(false);

            publisher.publishToUser(userId, event);

            verify(notificationService, never()).createNotification(
                    any(), any(), any(), any(), any(), any(), any(), any());
            verify(sseRegistry, never()).sendToUser(any(), any());
            verify(emailService, never()).sendNotificationEmail(any(), any());
        }

        @Test
        void skipsEmail_whenDisabled() {
            UUID userId = UUID.randomUUID();
            var event = createEvent();
            var savedEntity = createSavedEntity(userId);

            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.IN_APP))
                    .thenReturn(true);
            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.EMAIL))
                    .thenReturn(false);
            when(transactionManager.getTransaction(any()))
                    .thenReturn(new SimpleTransactionStatus());
            when(notificationService.createNotification(
                    eq(userId), eq("t"), eq("b"), any(), eq("SYSTEM"), eq("notification"), any(), any()))
                    .thenReturn(savedEntity);

            publisher.publishToUser(userId, event);

            verify(sseRegistry).sendToUser(eq(userId), any());
            verify(emailService, never()).sendNotificationEmail(any(), any());
        }

        @Test
        void skipsEmail_whenEmailIsNull() {
            UUID userId = UUID.randomUUID();
            var event = createEvent();

            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.IN_APP))
                    .thenReturn(false);
            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.EMAIL))
                    .thenReturn(true);
            when(userQueryPort.getEmailById(userId)).thenReturn(null);

            publisher.publishToUser(userId, event);

            verify(emailService, never()).sendNotificationEmail(any(), any());
        }

        @Test
        void skipsSse_whenSavedEntityIsNull() {
            UUID userId = UUID.randomUUID();
            var event = createEvent();

            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.IN_APP))
                    .thenReturn(true);
            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.EMAIL))
                    .thenReturn(false);
            when(transactionManager.getTransaction(any()))
                    .thenReturn(new SimpleTransactionStatus());
            when(notificationService.createNotification(
                    eq(userId), eq("t"), eq("b"), any(), eq("SYSTEM"), eq("notification"), any(), any()))
                    .thenReturn(null);

            publisher.publishToUser(userId, event);

            verify(sseRegistry, never()).sendToUser(any(), any());
        }

        @Test
        void catchesException_andLogsError() {
            UUID userId = UUID.randomUUID();
            var event = createEvent();

            when(preferenceService.isChannelEnabled(userId, event.category(), NotificationChannel.IN_APP))
                    .thenThrow(new RuntimeException("boom"));

            // Should not throw
            publisher.publishToUser(userId, event);
        }
    }

    @Nested
    class PublishToTier {

        @Test
        void resolvesUsersByTier() {
            when(userQueryPort.findActiveUserIdsByMinimumTier(CapabilityTier.ADMIN)).thenReturn(List.of());

            var event = new NotificationEvent("t", "b", null, NotificationCategory.ADMIN,
                    "user", null, null, null);
            publisher.publishToTier(CapabilityTier.ADMIN, event);

            verify(userQueryPort).findActiveUserIdsByMinimumTier(CapabilityTier.ADMIN);
        }

        @Test
        void deliversToEachUser() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            when(userQueryPort.findActiveUserIdsByMinimumTier(CapabilityTier.BASIC))
                    .thenReturn(List.of(userId1, userId2));
            var event = createEvent();
            when(preferenceService.isChannelEnabled(any(), any(), eq(NotificationChannel.IN_APP)))
                    .thenReturn(false);
            when(preferenceService.isChannelEnabled(any(), any(), eq(NotificationChannel.EMAIL)))
                    .thenReturn(false);

            publisher.publishToTier(CapabilityTier.BASIC, event);

            verify(preferenceService).isChannelEnabled(eq(userId1), any(), eq(NotificationChannel.IN_APP));
            verify(preferenceService).isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.IN_APP));
        }

        @Test
        void catchesOuterException() {
            when(userQueryPort.findActiveUserIdsByMinimumTier(any()))
                    .thenThrow(new RuntimeException("tier lookup failed"));

            publisher.publishToTier(CapabilityTier.ADMIN, createEvent());

            // Should not throw
        }

        @Test
        void catchesPerUserException_andContinues() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            var event = createEvent();
            when(userQueryPort.findActiveUserIdsByMinimumTier(CapabilityTier.BASIC))
                    .thenReturn(List.of(userId1, userId2));
            when(preferenceService.isChannelEnabled(eq(userId1), any(), eq(NotificationChannel.IN_APP)))
                    .thenThrow(new RuntimeException("user1 failed"));
            when(preferenceService.isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.IN_APP)))
                    .thenReturn(false);
            when(preferenceService.isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.EMAIL)))
                    .thenReturn(false);

            publisher.publishToTier(CapabilityTier.BASIC, event);

            // userId2 should still be processed despite userId1 failure
            verify(preferenceService).isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.IN_APP));
        }
    }

    @Nested
    class PublishToAll {

        @Test
        void resolvesAllUsers() {
            UUID userId = UUID.randomUUID();
            when(userQueryPort.findAllActiveUserIds()).thenReturn(List.of(userId));
            when(preferenceService.isChannelEnabled(eq(userId), any(), eq(NotificationChannel.IN_APP)))
                    .thenReturn(false);
            when(preferenceService.isChannelEnabled(eq(userId), any(), eq(NotificationChannel.EMAIL)))
                    .thenReturn(false);

            publisher.publishToAll(createEvent());

            verify(userQueryPort).findAllActiveUserIds();
        }

        @Test
        void deliversToEachUser() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            when(userQueryPort.findAllActiveUserIds()).thenReturn(List.of(userId1, userId2));
            var event = createEvent();
            when(preferenceService.isChannelEnabled(any(), any(), eq(NotificationChannel.IN_APP)))
                    .thenReturn(false);
            when(preferenceService.isChannelEnabled(any(), any(), eq(NotificationChannel.EMAIL)))
                    .thenReturn(false);

            publisher.publishToAll(event);

            verify(preferenceService).isChannelEnabled(eq(userId1), any(), eq(NotificationChannel.IN_APP));
            verify(preferenceService).isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.IN_APP));
        }

        @Test
        void catchesOuterException() {
            when(userQueryPort.findAllActiveUserIds())
                    .thenThrow(new RuntimeException("user lookup failed"));

            publisher.publishToAll(createEvent());

            // Should not throw
        }

        @Test
        void catchesPerUserException_andContinues() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            var event = createEvent();
            when(userQueryPort.findAllActiveUserIds()).thenReturn(List.of(userId1, userId2));
            when(preferenceService.isChannelEnabled(eq(userId1), any(), eq(NotificationChannel.IN_APP)))
                    .thenThrow(new RuntimeException("user1 failed"));
            when(preferenceService.isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.IN_APP)))
                    .thenReturn(false);
            when(preferenceService.isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.EMAIL)))
                    .thenReturn(false);

            publisher.publishToAll(event);

            // userId2 should still be processed
            verify(preferenceService).isChannelEnabled(eq(userId2), any(), eq(NotificationChannel.IN_APP));
        }
    }
}
