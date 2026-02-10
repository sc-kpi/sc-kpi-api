# module-audit

Audit logging module for administrative oversight.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-test` | Test |

## Components

### AuditController

Empty controller shell registered at `/api/v1/admin/audit-logs`. Swagger tag: **Audit** (admin only).

This endpoint falls under the `6-admin` OpenAPI group and is not included in `SecurityConstants.PUBLIC_URLS`, requiring authentication.

## Package Structure

```
ua.kpi.sc.audit
└── controller/
    └── AuditController.java
```
