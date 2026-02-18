# module-council

Council module for departments.

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
| `DepartmentController` | `/api/v1/departments` | Departments | `council.departments` |

**Note:** Departments are listed in `SecurityConstants.PUBLIC_URLS`, meaning their read endpoints will be publicly accessible without authentication once implemented.

## Package Structure

```
ua.kpi.sc.council
├── controller/
│   └── DepartmentController.java
├── dto/
│   ├── request/        # (empty — pending implementation)
│   └── response/       # (empty — pending implementation)
├── entity/             # (empty — pending implementation)
├── mapper/             # (empty — pending implementation)
├── repository/         # (empty — pending implementation)
└── service/            # (empty — pending implementation)
```
