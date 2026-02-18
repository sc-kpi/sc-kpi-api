# module-engagements

Engagements module for clubs and projects.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-data-jpa` | Implementation |
| `spring-boot-starter-test` | Test |

## Controllers

Both controllers are scaffolded with feature flags but have no endpoint methods implemented yet. Empty subdirectories (`dto/`, `entity/`, `mapper/`, `repository/`, `service/`) are prepared for future implementation.

| Controller | Path | Swagger Tag | Feature Flag |
|---|---|---|---|
| `ClubController` | `/api/v1/clubs` | Clubs | `engagements.clubs` |
| `ProjectController` | `/api/v1/projects` | Projects | `engagements.projects` |

**Note:** Clubs and projects are listed in `SecurityConstants.PUBLIC_URLS`, meaning their read endpoints will be publicly accessible without authentication once implemented.

## Package Structure

```
ua.kpi.sc.engagements
├── controller/
│   ├── ClubController.java
│   └── ProjectController.java
├── dto/
│   ├── request/        # (empty — pending implementation)
│   └── response/       # (empty — pending implementation)
├── entity/             # (empty — pending implementation)
├── mapper/             # (empty — pending implementation)
├── repository/         # (empty — pending implementation)
└── service/            # (empty — pending implementation)
```
