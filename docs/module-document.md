# module-document

Document management module.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-data-jpa` | Implementation |
| `spring-boot-starter-test` | Test |

## Controllers

Controller is scaffolded with a feature flag but has no endpoint methods implemented yet. Empty subdirectories (`dto/`, `entity/`, `mapper/`, `repository/`, `service/`) are prepared for future implementation.

| Controller | Path | Swagger Tag | Feature Flag |
|---|---|---|---|
| `DocumentController` | `/api/v1/documents` | Documents | `documents.management` |

## Package Structure

```
ua.kpi.sc.document
├── controller/
│   └── DocumentController.java
├── dto/
│   ├── request/        # (empty — pending implementation)
│   └── response/       # (empty — pending implementation)
├── entity/             # (empty — pending implementation)
├── mapper/             # (empty — pending implementation)
├── repository/         # (empty — pending implementation)
└── service/            # (empty — pending implementation)
```
