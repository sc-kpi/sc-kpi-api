package ua.kpi.sc.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.notification.NotificationCategory;
import ua.kpi.sc.common.notification.NotificationPublisher;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.UserPrincipal;
import ua.kpi.sc.notification.dto.BroadcastRequest;
import ua.kpi.sc.notification.dto.NotificationStatsResponse;
import ua.kpi.sc.notification.entity.NotificationEntity;
import ua.kpi.sc.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationAdminServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private NotificationService notificationService;
    @Mock private AuditPublisher auditPublisher;

    @InjectMocks
    private NotificationAdminService service;

    private UserPrincipal createAdmin() {
        return UserPrincipal.builder()
                .id(UUID.randomUUID()).email("admin@kpi.ua")
                .tier(CapabilityTier.ADMIN).active(true).build();
    }

    @Nested
    class GetAllNotifications {

        @Test
        @SuppressWarnings("unchecked")
        void returnsPage_withNoFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            var entity = NotificationEntity.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                    .titleKey("t").bodyKey("b").category("SYSTEM").sourceModule("notification").build();
            Page<NotificationEntity> page = new PageImpl<>(List.of(entity));
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getAllNotifications(null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().titleKey()).isEqualTo("t");
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_category() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getAllNotifications("SECURITY", null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_search() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getAllNotifications(null, "keyword", null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_dateRange() {
            Pageable pageable = PageRequest.of(0, 10);
            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getAllNotifications(null, null, from, to, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void filtersBy_allParameters() {
            Pageable pageable = PageRequest.of(0, 10);
            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getAllNotifications("ADMIN", "search", from, to, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void ignoresBlankCategory() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getAllNotifications("  ", null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void ignoresBlankSearch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<NotificationEntity> page = new PageImpl<>(List.of());
            when(notificationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            var result = service.getAllNotifications(null, "  ", null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class Broadcast {

        @Test
        void toAll_publishesToAll() {
            var request = new BroadcastRequest("title.key", "body.key", null,
                    NotificationCategory.SYSTEM, null);

            service.broadcast(request, createAdmin());

            verify(notificationPublisher).publishToAll(any());
            verify(auditPublisher).publish(any());
        }

        @Test
        void toTier_publishesToTier() {
            var request = new BroadcastRequest("title.key", "body.key", null,
                    NotificationCategory.ADMIN, "BASIC");

            service.broadcast(request, createAdmin());

            verify(notificationPublisher).publishToTier(any(), any());
            verify(auditPublisher).publish(any());
        }

        @Test
        void toAll_withBlankTier_publishesToAll() {
            var request = new BroadcastRequest("title.key", "body.key", null,
                    NotificationCategory.SYSTEM, "  ");

            service.broadcast(request, createAdmin());

            verify(notificationPublisher).publishToAll(any());
        }

        @Test
        void withBodyArgs_publishesEvent() {
            var request = new BroadcastRequest("title.key", "body.key",
                    new String[]{"arg1"}, NotificationCategory.SECURITY, null);

            service.broadcast(request, createAdmin());

            verify(notificationPublisher).publishToAll(any());
            verify(auditPublisher).publish(any());
        }
    }

    @Nested
    class GetStats {

        @Test
        void returnsCounts() {
            when(notificationRepository.count()).thenReturn(100L);
            when(notificationRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(10L);
            when(notificationRepository.findAll()).thenReturn(Collections.emptyList());

            NotificationStatsResponse stats = service.getStats();

            assertThat(stats.total()).isEqualTo(100L);
            assertThat(stats.last24h()).isEqualTo(10L);
            assertThat(stats.byCategory()).isEmpty();
        }

        @Test
        void returnsByCategory() {
            var entity1 = NotificationEntity.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                    .titleKey("t1").bodyKey("b1").category("SYSTEM").sourceModule("notification").build();
            var entity2 = NotificationEntity.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                    .titleKey("t2").bodyKey("b2").category("SYSTEM").sourceModule("notification").build();
            var entity3 = NotificationEntity.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                    .titleKey("t3").bodyKey("b3").category("SECURITY").sourceModule("auth").build();
            when(notificationRepository.count()).thenReturn(3L);
            when(notificationRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(3L);
            when(notificationRepository.findAll()).thenReturn(List.of(entity1, entity2, entity3));

            NotificationStatsResponse stats = service.getStats();

            assertThat(stats.total()).isEqualTo(3L);
            assertThat(stats.byCategory()).containsEntry("SYSTEM", 2L);
            assertThat(stats.byCategory()).containsEntry("SECURITY", 1L);
        }
    }

    @Nested
    class Cleanup {

        @Test
        void deletesAndAudits() {
            when(notificationService.deleteOldNotifications(any())).thenReturn(5);

            int deleted = service.cleanup(90, createAdmin());

            assertThat(deleted).isEqualTo(5);
            verify(auditPublisher).publish(any());
        }

        @Test
        void returnsZero_whenNothingToDelete() {
            when(notificationService.deleteOldNotifications(any())).thenReturn(0);

            int deleted = service.cleanup(30, createAdmin());

            assertThat(deleted).isZero();
            verify(auditPublisher).publish(any());
        }
    }
}
