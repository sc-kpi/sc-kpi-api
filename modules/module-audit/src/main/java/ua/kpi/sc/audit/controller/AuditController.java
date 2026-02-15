package ua.kpi.sc.audit.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.audit.dto.AuditEventResponse;
import ua.kpi.sc.audit.dto.AuditStatsResponse;
import ua.kpi.sc.audit.service.AuditService;
import ua.kpi.sc.common.audit.AuditAction;
import ua.kpi.sc.common.audit.AuditEntityType;
import ua.kpi.sc.common.audit.AuditEventBuilder;
import ua.kpi.sc.common.audit.AuditPublisher;
import ua.kpi.sc.common.featureflag.FeatureFlag;
import ua.kpi.sc.common.security.CapabilityTier;
import ua.kpi.sc.common.security.RequireTier;
import ua.kpi.sc.common.security.UserPrincipal;

/**
 * REST controller for admin-only audit log querying and export.
 *
 * @since 0.4.0
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Audit", description = "Audit log endpoints (admin only)")
@RequireTier(CapabilityTier.ADMIN)
@FeatureFlag("audit.system")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final AuditPublisher auditPublisher;

    @GetMapping
    public Page<AuditEventResponse> getAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String sourceModule,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return auditService.getAuditEvents(actorId, entityType, action, sourceModule, entityId,
                from, to, search, pageable);
    }

    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String sourceModule,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal requester,
            HttpServletResponse response) throws Exception {
        String filename = "audit-logs-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        auditService.exportCsv(actorId, entityType, action, sourceModule, entityId,
                from, to, search, response.getWriter());

        auditPublisher.publish(AuditEventBuilder.builder()
                .actor(requester)
                .action(AuditAction.AUDIT_EXPORTED)
                .entityType(AuditEntityType.AUTH)
                .sourceModule("audit")
                .details("CSV export")
                .build());
    }

    @GetMapping("/stats")
    public AuditStatsResponse getStats() {
        return auditService.getStats();
    }
}
