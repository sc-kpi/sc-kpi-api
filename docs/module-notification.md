# module-notification

Notification system module with Telegram bot webhook integration.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-test` | Test |

## Controllers

Both controllers are empty shells.

| Controller | Path | Swagger Tag |
|---|---|---|
| `NotificationSettingsController` | `/api/v1/notifications/settings` | Notifications |
| `TelegramWebhookController` | `/api/v1/webhooks/telegram` | Telegram Webhook |

## Package Structure

```
ua.kpi.sc.notification
└── controller/
    ├── NotificationSettingsController.java
    └── TelegramWebhookController.java
```
