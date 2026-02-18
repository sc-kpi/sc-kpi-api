# module-notification

Notification system module with in-app notifications, email delivery, user preferences, real-time SSE streaming, and admin broadcasting.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-data-jpa` | Implementation |
| `spring-boot-starter-mail` | Implementation |
| `spring-boot-starter-test` | Test |

## Components

### NotificationController

Registered at `/api/v1/notifications`. Requires **BASIC** tier. Swagger tag: **Notifications**.

| Method | Endpoint | Feature Flag | Purpose |
|---|---|---|---|
| GET | `/` | `notifications.enabled` | List user's notifications (paginated, filterable) |
| GET | `/unread-count` | `notifications.enabled` | Get unread count |
| PATCH | `/{id}/read` | `notifications.enabled` | Mark single as read |
| PATCH | `/mark-read` | `notifications.enabled` | Mark batch as read (list of IDs) |
| PATCH | `/mark-all-read` | `notifications.enabled` | Mark all as read |
| GET | `/stream` | `notifications.sse` | Open SSE connection |

### NotificationPreferencesController

Registered at `/api/v1/notifications/preferences`. Requires **BASIC** tier. Swagger tag: **Notifications**.

| Method | Endpoint | Feature Flag | Purpose |
|---|---|---|---|
| GET | `/` | `notifications.enabled` | Get all preferences |
| PUT | `/` | `notifications.enabled` | Update preferences (audited) |

### NotificationAdminController

Registered at `/api/v1/admin/notifications`. Requires **ADMIN** tier. Swagger tag: **Notifications** (admin only).

| Method | Endpoint | MFA | Feature Flag | Purpose |
|---|---|---|---|---|
| GET | `/` | No | `notifications.enabled` | List all notifications (filterable) |
| POST | `/broadcast` | Yes | `notifications.admin-broadcast` | Broadcast to users |
| GET | `/stats` | No | `notifications.enabled` | Get notification statistics |
| DELETE | `/cleanup` | No | `notifications.enabled` | Manually cleanup old notifications |

### TelegramWebhookController

Placeholder for future Telegram bot integration. Registered at `/api/v1/webhooks/telegram`. Feature flag: `notifications.telegram`.

### NotificationService

Core CRUD service: create, paginated query with filtering (category, read status, date range, text search), mark read (single/batch/all), delete. Enforces ownership validation.

### NotificationAdminService

Admin operations: retrieve all notifications, broadcast (tier-based or all users), statistics, retention-based cleanup.

### NotificationPreferenceService

Per-channel, per-category preferences with defaults. All channels enabled by default except EMAIL for FEATURE_FLAG category.

### NotificationPublisherImpl

Implements `NotificationPublisher` port from `common`. Routes through dual channels: IN_APP (persist + SSE push) and EMAIL (async). Error-safe: catches and logs but never throws.

### NotificationEmailService

Async email sending with retry logic (configurable max retries).

### SseEmitterRegistry

Thread-safe registry (`ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>`). Supports multiple connections per user (multi-tab/device). Auto-cleanup on completion, timeout, or error.

### SseHeartbeatScheduler

Scheduled task running every 30 seconds to keep SSE connections alive and detect dead emitters.

### NotificationCleanupScheduler

Runs daily (3 AM by default) to delete notifications older than the retention period. Configurable via cron expression.

### NotificationSpecification

Composable JPA specifications: `hasUserId`, `hasCategory`, `isRead`/`isUnread`, `createdAfter`/`createdBefore`, `searchByText`.

### NotificationInfoContributor

Actuator info contributor exposed at `/actuator/info`: total, last24h, activeConnections, activeUsers.

### Entities

| Entity | Key Fields |
|---|---|
| `NotificationEntity` | id (UUID), userId, titleKey, bodyKey, bodyArgs (String[]), category, sourceModule, relatedEntityId, relatedEntityType, read (boolean), readAt, createdAt |
| `NotificationPreferenceEntity` | id (UUID), userId, category, channel, enabled (boolean), updatedAt. Unique constraint: (user_id, category, channel) |

### Repositories

| Repository | Description |
|---|---|
| `NotificationRepository` | Standard CRUD + `JpaSpecificationExecutor` + custom queries (`countByUserIdAndReadFalse`, `deleteByCreatedAtBefore`, `countByCreatedAtAfter`) |
| `NotificationPreferenceRepository` | `findByUserId`, `findByUserIdAndCategoryAndChannel` |

### DTOs

| DTO | Type | Description |
|---|---|---|
| `NotificationResponse` | Response | Mirrors entity fields |
| `NotificationStatsResponse` | Response | total, last24h, activeConnections, activeUsers |
| `PreferenceResponse` | Response | category, channel, enabled |
| `BroadcastRequest` | Request | titleKey, bodyKey, category, minimumTier (optional) |
| `UpdatePreferencesRequest` | Request | List of preference updates (category, channel, enabled) |

### Audit Events

| Event |
|---|
| `NOTIFICATION_PREFERENCES_UPDATED` |
| `NOTIFICATION_BROADCAST` |
| `NOTIFICATION_CLEANUP` |

## Configuration Properties

| Property Path | Type | Default |
|---|---|---|
| `app.notifications.sse-timeout-ms` | `long` | `300000` (5 min) |
| `app.notifications.sse-heartbeat-interval-ms` | `long` | `30000` (30 sec) |
| `app.notifications.retention-days` | `int` | `90` |
| `app.notifications.email-max-retries` | `int` | `3` |
| `app.notifications.cleanup.enabled` | `boolean` | `true` |
| `app.notifications.cleanup.cron` | `String` | `0 0 3 * * ?` |

## Package Structure

```
ua.kpi.sc.notification
├── actuator/
│   └── NotificationInfoContributor.java
├── config/
│   ├── NotificationConfig.java
│   └── NotificationProperties.java
├── controller/
│   ├── NotificationController.java
│   ├── NotificationPreferencesController.java
│   ├── NotificationAdminController.java
│   └── TelegramWebhookController.java
├── dto/
│   ├── request/
│   │   ├── BroadcastRequest.java
│   │   └── UpdatePreferencesRequest.java
│   └── response/
│       ├── NotificationResponse.java
│       ├── NotificationStatsResponse.java
│       └── PreferenceResponse.java
├── entity/
│   ├── NotificationEntity.java
│   └── NotificationPreferenceEntity.java
├── repository/
│   ├── NotificationRepository.java
│   └── NotificationPreferenceRepository.java
├── service/
│   ├── NotificationService.java
│   ├── NotificationAdminService.java
│   ├── NotificationPreferenceService.java
│   ├── NotificationPublisherImpl.java
│   └── NotificationEmailService.java
├── specification/
│   └── NotificationSpecification.java
└── sse/
    ├── SseEmitterRegistry.java
    └── SseHeartbeatScheduler.java
```
