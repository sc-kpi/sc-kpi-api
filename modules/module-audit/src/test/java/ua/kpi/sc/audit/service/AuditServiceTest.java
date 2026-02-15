package ua.kpi.sc.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import ua.kpi.sc.audit.entity.AuditEventEntity;
import ua.kpi.sc.audit.repository.AuditEventRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    @InjectMocks
    private AuditService service;

    private AuditEventEntity sampleEntity() {
        return AuditEventEntity.builder()
                .id(UUID.randomUUID())
                .actorId(UUID.randomUUID())
                .actorEmail("admin@kpi.ua")
                .action("CREATED")
                .entityType("USER")
                .entityId(UUID.randomUUID())
                .entityName("test@kpi.ua")
                .sourceModule("user")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAuditEvents_returnsPagedResults() {
        var entity = sampleEntity();
        var pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = service.getAuditEvents(null, null, null, null, null,
                null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().action()).isEqualTo("CREATED");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAuditEvents_withAllFilters() {
        var pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.getAuditEvents(
                UUID.randomUUID(), "USER", "CREATED", "user", UUID.randomUUID(),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(), "test",
                pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportCsv_writesHeaderAndRows() {
        var entity = sampleEntity();
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        service.exportCsv(null, null, null, null, null, null, null, null, pw);

        String csv = sw.toString();
        assertThat(csv).contains("Timestamp,Actor,Action,Entity Type");
        assertThat(csv).contains("CREATED");
        assertThat(csv).contains("admin@kpi.ua");
    }

    @Test
    void getStats_returnsCounts() {
        when(repository.count()).thenReturn(100L);
        when(repository.countByCreatedAtAfter(any(Instant.class))).thenReturn(10L);
        var entity1 = sampleEntity();
        entity1.setEntityType("USER");
        entity1.setAction("CREATED");
        var entity2 = sampleEntity();
        entity2.setEntityType("FEATURE_FLAG");
        entity2.setAction("TOGGLED");
        when(repository.findAll()).thenReturn(List.of(entity1, entity2));

        var stats = service.getStats();

        assertThat(stats.totalEvents()).isEqualTo(100);
        assertThat(stats.eventsLast24h()).isEqualTo(10);
        assertThat(stats.byEntityType()).containsKeys("USER", "FEATURE_FLAG");
        assertThat(stats.byAction()).containsKeys("CREATED", "TOGGLED");
    }
}
