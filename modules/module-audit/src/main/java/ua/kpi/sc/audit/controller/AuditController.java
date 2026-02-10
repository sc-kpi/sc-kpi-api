package ua.kpi.sc.audit.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for admin-only audit log querying and export.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Audit", description = "Audit log endpoints (admin only)")
public class AuditController {
}
