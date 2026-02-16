package ua.kpi.sc.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ua.kpi.sc.common.exception.ForbiddenException;
import ua.kpi.sc.common.exception.ResourceNotFoundException;
import ua.kpi.sc.notification.entity.NotificationEntity;
import ua.kpi.sc.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService service;

    @Nested
    class CreateNotification {

        @Test
        void savesEntity() {
            UUID userId = UUID.randomUUID();
            var entity = NotificationEntity.builder().id(UUID.randomUUID()).userId(userId)
                    .titleKey("test.title").bodyKey("test.body").category("SECURITY")
                    .sourceModule("auth").build();
            when(notificationRepository.save(any())).thenReturn(entity);

            var result = service.createNotification(userId, "test.title", "test.body",
                    null, "SECURITY", "auth", null, null);

            assertThat(result.getTitleKey()).isEqualTo("test.title");
            verify(notificationRepository).save(any());
        }

        @Test
        void savesEntityWithAllFields() {
            UUID userId = UUID.randomUUID();
            UUID relatedId = UUID.randomUUID();
            String[] bodyArgs = {"arg1", "arg2"};
            var entity = NotificationEntity.builder().id(UUID.randomUUID()).userId(userId)
                    .titleKey("test.title").bodyKey("test.body").bodyArgs(bodyArgs)
                    .category("ADMIN").sourceModule("user")
                    .relatedEntityId(relatedId).relatedEntityType("USER").build();
            when(notificationRepository.save(any())).thenReturn(entity);

            var result = service.createNotification(userId, "test.title", "test.body",
                    bodyArgs, "ADMIN", "user", relatedId, "USER");

            assertThat(result.getCategory()).isEqualTo("ADMIN");
            assertThat(result.getRelatedEntityId()).isEqualTo(relatedId);
            assertThat(result.getRelatedEntityType()).isEqualTo("USER");
            assertThat(result.getBodyArgs()).containsExactly("arg1", "arg2");
        }
    }

    @Nested
    class GetUserNotifications {

        @Test
        @SuppressWarnings("unchecked")
        void returnsPagedNotifications_withNoFilters() {
            UUID userId = UUID.randomUUID();
            var entity = NotificationEntity.builder().id(UUID.randomUUID()).userId(userId)
                    .titleKey("t").bodyKey("b").category("SYSTEM").sourceModule("notification").build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of(entity));
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getUserNotifications(userId, null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().titleKey()).isEqualTo("t");
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_category() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getUserNotifications(userId, "SECURITY", null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(notificationRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_isRead() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getUserNotifications(userId, null, true, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_dateRange() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getUserNotifications(userId, null, null, from, to, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_allParameters() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getUserNotifications(userId, "ADMIN", false, from, to, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void ignoresBlankCategory() {
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getUserNotifications(userId, "  ", null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetUnreadCount {

        @Test
        void delegatesToRepository() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

            assertThat(service.getUnreadCount(userId)).isEqualTo(5L);
        }

        @Test
        void returnsZero_whenNoUnread() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(0L);

            assertThat(service.getUnreadCount(userId)).isZero();
        }
    }

    @Nested
    class MarkAsRead {

        @Test
        void throwsNotFound_whenMissing() {
            UUID userId = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsRead(userId, notifId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsForbidden_whenWrongUser() {
            UUID userId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            var entity = NotificationEntity.builder().id(notifId).userId(otherId)
                    .titleKey("t").bodyKey("b").category("SECURITY").sourceModule("auth").build();
            when(notificationRepository.findById(notifId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.markAsRead(userId, notifId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void setsReadFlag() {
            UUID userId = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            var entity = NotificationEntity.builder().id(notifId).userId(userId)
                    .titleKey("t").bodyKey("b").category("SECURITY").sourceModule("auth").build();
            when(notificationRepository.findById(notifId)).thenReturn(Optional.of(entity));
            when(notificationRepository.save(any())).thenReturn(entity);

            service.markAsRead(userId, notifId);

            assertThat(entity.isRead()).isTrue();
            assertThat(entity.getReadAt()).isNotNull();
            verify(notificationRepository).save(entity);
        }

        @Test
        void skipsAlreadyReadNotification() {
            UUID userId = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            var entity = NotificationEntity.builder().id(notifId).userId(userId)
                    .titleKey("t").bodyKey("b").category("SECURITY").sourceModule("auth")
                    .read(true).readAt(Instant.now()).build();
            when(notificationRepository.findById(notifId)).thenReturn(Optional.of(entity));

            service.markAsRead(userId, notifId);

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    class MarkBatchAsRead {

        @Test
        void marksMultipleNotifications() {
            UUID userId = UUID.randomUUID();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            var entity1 = NotificationEntity.builder().id(id1).userId(userId)
                    .titleKey("t1").bodyKey("b1").category("SYSTEM").sourceModule("notification").build();
            var entity2 = NotificationEntity.builder().id(id2).userId(userId)
                    .titleKey("t2").bodyKey("b2").category("SYSTEM").sourceModule("notification").build();
            when(notificationRepository.findById(id1)).thenReturn(Optional.of(entity1));
            when(notificationRepository.findById(id2)).thenReturn(Optional.of(entity2));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.markBatchAsRead(userId, List.of(id1, id2));

            assertThat(entity1.isRead()).isTrue();
            assertThat(entity2.isRead()).isTrue();
            verify(notificationRepository, times(2)).save(any());
        }

        @Test
        void handlesEmptyList() {
            UUID userId = UUID.randomUUID();

            service.markBatchAsRead(userId, List.of());

            verify(notificationRepository, never()).findById(any());
        }
    }

    @Nested
    class MarkAllAsRead {

        @Test
        void delegatesToRepository() {
            UUID userId = UUID.randomUUID();
            when(notificationRepository.markAllReadByUserId(eq(userId), any(Instant.class))).thenReturn(3);

            int result = service.markAllAsRead(userId);

            assertThat(result).isEqualTo(3);
            verify(notificationRepository).markAllReadByUserId(eq(userId), any(Instant.class));
        }
    }

    @Nested
    class DeleteOldNotifications {

        @Test
        void delegatesToRepository() {
            Instant cutoff = Instant.now().minusSeconds(86400);
            when(notificationRepository.deleteOlderThan(cutoff)).thenReturn(10);

            int result = service.deleteOldNotifications(cutoff);

            assertThat(result).isEqualTo(10);
            verify(notificationRepository).deleteOlderThan(cutoff);
        }
    }
}
