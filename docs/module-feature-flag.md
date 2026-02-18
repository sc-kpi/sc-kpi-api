# module-feature-flag

Feature flag management and evaluation module. Provides public flag evaluation endpoints, admin CRUD for flags and overrides, AOP method gating via `@FeatureFlag` annotation, and Caffeine caching.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-data-jpa` | Implementation |
| `com.github.ben-manes.caffeine:caffeine` | Implementation |
| `spring-boot-starter-test` | Test |

## Components

### Controllers

#### FeatureFlagController

Base path: `/api/v1/feature-flags`. Swagger tag: **Feature Flags** (public — no auth required).

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/` | Evaluate all flags for current user -> `Map<String, Boolean>` |
| GET | `/{key}` | Evaluate single flag -> `FeatureFlagEvaluationResponse` |

#### FeatureFlagAdminController

Base path: `/api/v1/admin/feature-flags`. Swagger tag: **Feature Flags Admin**. Requires ADMIN tier + MFA.

| Method | Endpoint | Status | Purpose |
|---|---|---|---|
| GET | `/` | 200 | List flags (paginated) |
| POST | `/` | 201 | Create flag |
| GET | `/{id}` | 200 | Get flag details |
| PATCH | `/{id}` | 200 | Update flag |
| PATCH | `/{id}/toggle` | 200 | Toggle flag enabled/disabled |
| DELETE | `/{id}` | 204 | Delete flag |
| POST | `/{id}/overrides` | 201 | Add override (user or tier) |
| DELETE | `/{id}/overrides/{overrideId}` | 204 | Remove override |
| POST | `/bulk-toggle` | 200 | Bulk toggle multiple flags |

### Services

#### FeatureFlagService

CRUD service for flags and overrides. Publishes audit events and notifications on all mutations. Evicts cache on changes.

#### FeatureFlagEvaluationService

Flag evaluation with 6-tier priority:

| Priority | Source | Description |
|---|---|---|
| 1 | Config overrides | `feature-flags.overrides` map (highest priority) |
| 2 | User-specific DB override | Override targeting a specific user ID |
| 3 | Tier-specific DB override | Override targeting a specific tier level |
| 4 | Percentage rollout | Hash-based rollout using `rolloutPercentage` |
| 5 | Flag enabled state | The flag's `enabled` boolean |
| 6 | Config defaults | `feature-flags.defaults` map (lowest priority) |

Uses `@Cacheable` with composite key: `key:userId:tierLevel`.

### AOP Components

#### FeatureFlagMethodInterceptor

`MethodInterceptor` that gates methods and classes annotated with `@FeatureFlag`. Throws `ResourceNotFoundException` (404) if the flag is disabled for the current user.

#### FeatureFlagInterceptorConfig

`@Configuration` class that creates an `Advisor` bean for `@FeatureFlag` annotation interception.

#### FeatureFlagCheckerAdapter

Implements the `FeatureFlagChecker` port defined in `common`. Bridges evaluation requests to `FeatureFlagEvaluationService`.

### Entities

#### FeatureFlag

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | Primary key |
| `key` | `String` | Unique, pattern: `[a-z0-9]+(\.[a-z0-9]+)*` |
| `name` | `String` | Human-readable name |
| `description` | `String` | Optional description |
| `enabled` | `boolean` | Whether the flag is globally enabled |
| `environment` | `String` | Target environment |
| `rolloutPercentage` | `Integer` | Percentage rollout (0-100) |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last update timestamp |
| `overrides` | `List<FeatureFlagOverride>` | OneToMany relationship |

#### FeatureFlagOverride

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | Primary key |
| `flag` | `FeatureFlag` | ManyToOne relationship |
| `overrideType` | `OverrideType` | `TIER` or `USER` |
| `tierLevel` | `Integer` | Target tier level (nullable, used when type is `TIER`) |
| `userId` | `UUID` | Target user ID (nullable, used when type is `USER`) |
| `enabled` | `boolean` | Override value |
| `createdAt` | `Instant` | Creation timestamp |

#### OverrideType

Enum: `TIER`, `USER`.

### DTOs

#### Request DTOs

| DTO | Fields |
|---|---|
| `CreateFeatureFlagRequest` | `key` (validated pattern), `name`, `enabled`, `environment`, `rolloutPercentage` (0-100) |
| `UpdateFeatureFlagRequest` | `name`, `description`, `enabled`, `environment`, `rolloutPercentage` (all optional partial updates) |
| `ToggleFeatureFlagRequest` | `enabled`, `reason` |
| `CreateOverrideRequest` | `overrideType`, `tierLevel`, `userId`, `enabled` (with validation) |
| `BulkToggleRequest` | `keys` (`List<String>`), `enabled`, `reason` |

#### Response DTOs

| DTO | Fields |
|---|---|
| `FeatureFlagResponse` | Complete flag state including list of `OverrideResponse` |
| `FeatureFlagEvaluationResponse` | `key`, `enabled` |
| `OverrideResponse` | `id`, `overrideType`, `tierLevel`, `userId`, `enabled` |

### Repositories

#### FeatureFlagRepository

| Method | Description |
|---|---|
| `findByKey()` | Find flag by unique key |
| `existsByKey()` | Check if a flag with the given key exists |
| `findAllWithOverrides()` | Find all flags with overrides eagerly loaded (EntityGraph) |
| `findByKeyWithOverrides()` | Find flag by key with overrides eagerly loaded (EntityGraph) |

#### FeatureFlagOverrideRepository

| Method | Description |
|---|---|
| `findByFlagIdAndOverrideTypeAndTierLevel()` | Find tier-specific override |
| `findByFlagIdAndOverrideTypeAndUserId()` | Find user-specific override |
| `deleteByFlagIdAndOverrideTypeAndTierLevel()` | Delete tier-specific override |
| `deleteByFlagIdAndOverrideTypeAndUserId()` | Delete user-specific override |

### Actuator

#### FeatureFlagInfoContributor

Implements `InfoContributor` to expose feature flag statistics at `/actuator/info`:

| Metric | Description |
|---|---|
| `totalFlags` | Total number of feature flags |
| `enabledFlags` | Number of enabled flags |
| `disabledFlags` | Number of disabled flags |

### Audit Events Published

The module publishes the following audit events via `AuditPublisher` (all with entityType `FEATURE_FLAG`):

| Event | Trigger |
|---|---|
| `CREATED` | New flag created |
| `UPDATED` | Flag properties updated |
| `TOGGLED` | Flag enabled/disabled via toggle endpoint |
| `DELETED` | Flag deleted |
| `OVERRIDE_ADDED` | Override added to a flag |
| `OVERRIDE_REMOVED` | Override removed from a flag |
| `BULK_TOGGLED` | Multiple flags toggled at once |

## Configuration Properties

### FeatureFlagProperties

`@ConfigurationProperties(prefix = "feature-flags")` — Feature flag configuration:

| Property | Type | Default | Description |
|---|---|---|---|
| `feature-flags.overrides` | `Map<String, Boolean>` | `{}` | Config-level overrides (highest evaluation priority) |
| `feature-flags.defaults` | `Map<String, Boolean>` | `{}` | Config-level defaults (lowest evaluation priority) |
| `feature-flags.cache-ttl-seconds` | `int` | `30` | Cache TTL in seconds |

### FeatureFlagCacheConfig

Caffeine CacheManager configuration:

| Setting | Value |
|---|---|
| TTL | 30 seconds (from `cacheTtlSeconds`) |
| Max entries | 500 |

## Package Structure

```
ua.kpi.sc.featureflag
├── actuator/
│   └── FeatureFlagInfoContributor.java
├── adapter/
│   └── FeatureFlagCheckerAdapter.java
├── config/
│   ├── FeatureFlagCacheConfig.java
│   ├── FeatureFlagInterceptorConfig.java
│   └── FeatureFlagProperties.java
├── controller/
│   ├── FeatureFlagController.java
│   └── FeatureFlagAdminController.java
├── dto/
│   ├── request/
│   │   ├── CreateFeatureFlagRequest.java
│   │   ├── UpdateFeatureFlagRequest.java
│   │   ├── ToggleFeatureFlagRequest.java
│   │   ├── CreateOverrideRequest.java
│   │   └── BulkToggleRequest.java
│   └── response/
│       ├── FeatureFlagResponse.java
│       ├── FeatureFlagEvaluationResponse.java
│       └── OverrideResponse.java
├── entity/
│   ├── FeatureFlag.java
│   ├── FeatureFlagOverride.java
│   └── OverrideType.java
├── interceptor/
│   └── FeatureFlagMethodInterceptor.java
├── repository/
│   ├── FeatureFlagRepository.java
│   └── FeatureFlagOverrideRepository.java
└── service/
    ├── FeatureFlagService.java
    └── FeatureFlagEvaluationService.java
```
