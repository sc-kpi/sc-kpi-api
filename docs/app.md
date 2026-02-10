# app

Application entry point module. Aggregates all domain modules into a single deployable Spring Boot JAR and provides cross-cutting configuration.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `module-auth` | Implementation (project) |
| `module-user` | Implementation (project) |
| `module-engagements` | Implementation (project) |
| `module-council` | Implementation (project) |
| `module-document` | Implementation (project) |
| `module-notification` | Implementation (project) |
| `module-audit` | Implementation (project) |
| `spring-boot-starter-web` | Implementation |
| `spring-boot-starter-security` | Implementation |
| `spring-boot-starter-actuator` | Implementation |
| `springdoc-openapi-starter-webmvc-ui` 3.0.1 | Implementation |
| `spring-boot-devtools` | Development only |
| `spring-boot-starter-test` | Test |
| `spring-security-test` | Test |

## Components

### ScKpiApiApplication

Main class annotated with `@SpringBootApplication(scanBasePackages = "ua.kpi.sc")`. Scans all modules under the `ua.kpi.sc` package.

### AsyncConfig

`@Configuration` + `@EnableAsync`. Exposes a `ThreadPoolTaskExecutor` bean named `taskExecutor`:

| Property | Env Variable | Default |
|---|---|---|
| Core pool size | `ASYNC_CORE_POOL_SIZE` | `4` |
| Max pool size | `ASYNC_MAX_POOL_SIZE` | `8` |
| Queue capacity | `ASYNC_QUEUE_CAPACITY` | `100` |

Thread name prefix: `sc-kpi-async-`.

### OpenApiConfig

Configures SpringDoc OpenAPI with:

- **Global info**: Title "Student Council KPI API", version 1.0.0, DCT Team contact.
- **Security scheme**: Bearer JWT (`bearerAuth`), applied globally.
- **Grouped APIs**:

| Group | Paths |
|---|---|
| `1-auth` | `/api/v1/auth/**` |
| `2-users` | `/api/v1/users/**` |
| `3-engagements` | `/api/v1/clubs/**`, `/api/v1/projects/**` |
| `4-council` | `/api/v1/departments/**` |
| `5-documents` | `/api/v1/documents/**` |
| `6-notifications` | `/api/v1/notifications/**`, `/api/v1/webhooks/**` |
| `7-admin` | `/api/v1/admin/**` |

## Profiles

### dev (`application-dev.yml`)

| Setting | Value |
|---|---|
| Logging `ua.kpi.sc` | `DEBUG` |
| Logging `org.springframework.security` | `DEBUG` |
| DevTools restart | Enabled |
| DevTools livereload | Enabled |
| Swagger UI | Enabled |

### prod (`application-prod.yml`)

| Setting | Value |
|---|---|
| Logging `ua.kpi.sc` | `WARN` |
| Logging `org.springframework` | `WARN` |
| Swagger UI | Disabled |
| API docs | Disabled |

### test (`application-test.yml`)

| Setting | Value |
|---|---|
| Logging `ua.kpi.sc` | `DEBUG` |

## Actuator

Exposed endpoints: `health`, `info`, `metrics`. Health details shown when authorized.

## Boot JAR

Output: `app/build/libs/sc-kpi-api.jar`

## Package Structure

```
ua.kpi.sc
├── ScKpiApiApplication.java
└── config/
    ├── AsyncConfig.java
    └── OpenApiConfig.java

resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── application-test.yml
```
