package ua.kpi.sc.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ua.kpi.sc.audit.dto.AuditEventResponse;
import ua.kpi.sc.audit.dto.AuditStatsResponse;
import ua.kpi.sc.audit.service.AuditService;
import ua.kpi.sc.common.audit.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    private AuditController controller;

    @Test
    void getAuditLogs_delegatesToService() {
        var pageable = PageRequest.of(0, 20);
        var response = new AuditEventResponse(
                UUID.randomUUID(), UUID.randomUUID(), "admin@kpi.ua",
                "CREATED", "USER", UUID.randomUUID(), "test@kpi.ua",
                null, null, null, null, "user", null, Instant.now()
        );
        when(auditService.getAuditEvents(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        var result = controller.getAuditLogs(null, null, null, null, null,
                null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().action()).isEqualTo("CREATED");
    }

    @Test
    void getStats_delegatesToService() {
        var stats = new AuditStatsResponse(100, 10, Map.of("USER", 50L), Map.of("CREATED", 30L));
        when(auditService.getStats()).thenReturn(stats);

        var result = controller.getStats();

        assertThat(result.totalEvents()).isEqualTo(100);
        assertThat(result.eventsLast24h()).isEqualTo(10);
        verify(auditService).getStats();
    }
}
