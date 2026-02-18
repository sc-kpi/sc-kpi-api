# module-audit

Audit logging module for administrative oversight. Provides paginated query, CSV export, and statistics for all audit events.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-data-jpa` | Implementation |
| `spring-boot-starter-test` | Test |

## Components

### AuditController

REST controller registered at `/api/v1/admin/audit-logs`. Swagger tag: **Audit** (admin only). Requires ADMIN tier. Feature flag: `admin.audit`.

| Method | Endpoint | MFA | Purpose |
|---|---|---|---|
| GET | `/` | No | Query paginated audit logs with 9 optional filters |
| GET | `/export` | Yes | Export filtered logs as CSV |
| GET | `/stats` | No | Get audit statistics |

#### Query Filters

| Parameter | Type | Description |
|---|---|---|
| `actorId` | `UUID` | Filter by acting user ID |
| `entityType` | `AuditEntityType` | Filter by entity type enum |
| `action` | `AuditAction` | Filter by action enum |
| `sourceModule` | `String` | Filter by originating module |
| `entityId` | `String` | Filter by target entity ID |
| `createdAfter` | `Instant` | Events created after this timestamp |
| `createdBefore` | `Instant` | Events created before this timestamp |
| `search` | `String` | Free-text search (case-insensitive LIKE on email/name/details) |
| `Pageable` | — | Standard Spring pagination and sorting |

### AuditService

`@Transactional(readOnly = true)` service providing core audit query logic:

| Method | Description |
|---|---|
| `getAuditEvents()` | Paginated and filtered audit log retrieval using JPA Specifications |
| `exportCsv()` | Export filtered audit logs as CSV with proper escaping |
| `getStats()` | Aggregate counts by entity type and action |

### AuditPublisherImpl

Implements the `AuditPublisher` port defined in `common`. Persists audit events in an isolated transaction (`PROPAGATION_REQUIRES_NEW`). Catches all failures to prevent audit logging from breaking business logic.

### AuditEventEntity

JPA entity with 13 fields:

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Primary key |
| `actorId` | `UUID` | ID of the user who performed the action |
| `actorEmail` | `String` | Email of the acting user |
| `action` | `AuditAction` | Enum representing the action type |
| `entityType` | `AuditEntityType` | Enum representing the target entity type |
| `entityId` | `String` | ID of the affected entity |
| `entityName` | `String` | Human-readable name of the affected entity |
| `fieldName` | `String` | Name of the changed field (if applicable) |
| `oldValue` | `String` | Previous value of the field |
| `newValue` | `String` | New value of the field |
| `details` | `String` | Additional context or description |
| `sourceModule` | `String` | Module that generated the event |
| `ipAddress` | `String` | IP address of the request |
| `createdAt` | `Instant` | Timestamp of the event |

### DTOs

| DTO | Description |
|---|---|
| `AuditEventResponse` | Record mirroring entity fields (13 fields) |
| `AuditStatsResponse` | `totalEvents`, `eventsLast24h`, `byEntityType` (`Map<String, Long>`), `byAction` (`Map<String, Long>`) |

### AuditEventRepository

`JpaRepository` + `JpaSpecificationExecutor`. Custom queries:

| Method | Description |
|---|---|
| `countByCreatedAtAfter()` | Count events after a given timestamp |
| `findLatestEventTimestamp()` | Find the most recent event timestamp |

### AuditEventSpecification

9 composable static specifications for dynamic query building:

| Specification | Description |
|---|---|
| `always()` | No-op spec (matches all) |
| `hasActorId()` | Filter by actor UUID |
| `hasEntityType()` | Filter by entity type enum |
| `hasAction()` | Filter by action enum |
| `hasSourceModule()` | Filter by source module string |
| `hasEntityId()` | Filter by entity ID |
| `createdAfter()` | Events after a given timestamp |
| `createdBefore()` | Events before a given timestamp |
| `searchByText()` | Case-insensitive LIKE search on email, name, and details |

### AuditInfoContributor

Spring Boot Actuator `InfoContributor` exposing audit metadata at `/actuator/info`:

| Info Key | Description |
|---|---|
| `totalEvents` | Total number of audit events |
| `eventsLast24h` | Number of events in the last 24 hours |
| `latestEventTimestamp` | Timestamp of the most recent event |

## Configuration Properties

| Property Path | Env Variable | Default |
|---|---|---|
| `features.admin.audit` | `FEATURES_ADMIN_AUDIT` | `true` |

## Package Structure

```
ua.kpi.sc.audit
├── actuator/
│   └── AuditInfoContributor.java
├── controller/
│   └── AuditController.java
├── dto/
│   └── response/
│       ├── AuditEventResponse.java
│       └── AuditStatsResponse.java
├── entity/
│   └── AuditEventEntity.java
├── repository/
│   ├── AuditEventRepository.java
│   └── AuditEventSpecification.java
└── service/
    ├── AuditService.java
    └── AuditPublisherImpl.java
```
