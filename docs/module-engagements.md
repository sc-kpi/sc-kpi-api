# module-engagements

Engagements module for clubs and projects.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-test` | Test |

## Controllers

All controllers are empty shells exposing their respective base paths.

| Controller | Path | Swagger Tag |
|---|---|---|
| `ClubController` | `/api/v1/clubs` | Clubs |
| `ProjectController` | `/api/v1/projects` | Projects |

**Note:** Clubs and projects are listed in `SecurityConstants.PUBLIC_URLS`, meaning their read endpoints are publicly accessible without authentication.

## Package Structure

```
ua.kpi.sc.engagements
└── controller/
    ├── ClubController.java
    └── ProjectController.java
```
