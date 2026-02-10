# module-council

Council module for departments.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-test` | Test |

## Controllers

All controllers are empty shells exposing their respective base paths.

| Controller | Path | Swagger Tag |
|---|---|---|
| `DepartmentController` | `/api/v1/departments` | Departments |

**Note:** Departments are listed in `SecurityConstants.PUBLIC_URLS`, meaning their read endpoints are publicly accessible without authentication.

## Package Structure

```
ua.kpi.sc.council
└── controller/
    └── DepartmentController.java
```
