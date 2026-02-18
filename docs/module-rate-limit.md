# module-rate-limit

Rate limiting module with token bucket algorithm. Provides admin management of rate limit rules, a servlet filter for request enforcement, and Caffeine-based bucket storage.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-data-jpa` | Implementation |
| `com.bucket4j:bucket4j_jdk17-core` | Implementation (token bucket) |
| `com.github.ben-manes.caffeine:caffeine` | Implementation |
| `spring-boot-starter-test` | Test |

## Components

### RateLimitAdminController

REST controller registered at `/api/v1/admin/rate-limits`. Requires **ADMIN** tier + MFA. Feature flag: `admin.rate-limits`.

| Method | Endpoint | Status | Purpose |
|---|---|---|---|
| GET | `/` | 200 | List rules (paginated) |
| POST | `/` | 201 | Create rule |
| GET | `/{id}` | 200 | Get rule details |
| PATCH | `/{id}` | 200 | Update rule |
| PATCH | `/{id}/toggle` | 200 | Toggle rule enabled/disabled |
| DELETE | `/{id}` | 204 | Delete rule |
| GET | `/stats` | 200 | Get violation statistics |

### RateLimitService

CRUD service for rate limit rules. Publishes audit events and notifications on mutations. Invalidates bucket cache on changes.

### RateLimitEvaluationService

Rule matching (endpoint pattern via `AntPathMatcher` + scope) and token bucket consumption. Tracks violations. Returns rate limit headers.

### RateLimitFilter

Extends `OncePerRequestFilter`. Enforces rate limits on every request. Skips if rate limiting is disabled or no matching rule. On violation (bucket empty): returns 429 with `Retry-After` header.

Response headers set on every request:

| Header | Description |
|---|---|
| `X-RateLimit-Limit` | Maximum requests in window |
| `X-RateLimit-Remaining` | Remaining tokens |
| `X-RateLimit-Reset` | Time until bucket refill (seconds) |
| `Retry-After` | Seconds until retry (only on 429) |

### BucketStore

Interface defining bucket storage operations: `resolveBucket(key, burstCapacity, refillRate, refillDuration)`, `invalidateAll()`, `invalidateByPrefix(prefix)`, `estimatedSize()`.

### CaffeineBucketStore

In-memory `BucketStore` implementation using Caffeine cache. Configurable max bucket count and TTL.

### RateLimitRule

JPA entity with 12 fields:

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Primary key |
| `name` | `String` | Unique rule name |
| `endpointPattern` | `String` | Ant-style path pattern |
| `httpMethod` | `String` | HTTP method filter (nullable — matches all if null) |
| `scope` | `RateLimitScope` | Scope of rate limiting |
| `burstCapacity` | `int` | Maximum burst tokens |
| `refillRate` | `int` | Tokens added per refill |
| `refillDurationSeconds` | `int` | Refill interval in seconds |
| `enabled` | `boolean` | Whether the rule is active |
| `priority` | `int` | Rule evaluation priority |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last update timestamp |

### RateLimitScope (Enum)

| Value | Description |
|---|---|
| `GLOBAL` | Shared bucket for all requests |
| `IP` | Bucket per client IP |
| `USER` | Bucket per authenticated user |
| `TIER` | Bucket per user tier |

### DTOs

| DTO | Type | Description |
|---|---|---|
| `RateLimitRuleResponse` | Response | Mirrors entity fields |
| `CreateRateLimitRuleRequest` | Request | name, endpointPattern, httpMethod, scope, burstCapacity, refillRate, refillDurationSeconds, enabled, priority |
| `UpdateRateLimitRuleRequest` | Request | All fields optional for partial update |
| `RateLimitToggleRequest` | Request | enabled, reason |
| `RateLimitStatsResponse` | Response | totalViolations, violationsByRule (`Map`), violationsByScope (`Map`), activeBuckets |

### RateLimitRuleRepository

`JpaRepository` with custom queries:

| Method | Description |
|---|---|
| `existsByName()` | Check for duplicate rule name |
| `findByEnabledTrue()` | Retrieve all active rules |

### RateLimitCacheConfig

Caffeine `CacheManager` for rules caching (30s TTL, 100 max entries).

### RateLimitFilterRegistration

Registers `RateLimitFilter` bean and `CaffeineBucketStore` bean.

### RateLimitInfoContributor

Spring Boot Actuator `InfoContributor` exposed at `/actuator/info`:

| Info Key | Description |
|---|---|
| `enabled` | Whether rate limiting is enabled |
| `storageType` | Bucket storage type |
| `totalRules` | Total number of rate limit rules |
| `enabledRules` | Number of active rules |
| `activeBuckets` | Number of active buckets in cache |
| `totalViolations` | Total recorded violations |

### Audit Events

| Event |
|---|
| `CREATED` (entityType: RATE_LIMIT_RULE) |
| `UPDATED` (entityType: RATE_LIMIT_RULE) |
| `TOGGLED` (entityType: RATE_LIMIT_RULE) |
| `DELETED` (entityType: RATE_LIMIT_RULE) |

## Configuration Properties

| Property Path | Type | Default |
|---|---|---|
| `rate-limit.enabled` | `boolean` | `true` |
| `rate-limit.storage-type` | `String` | `"caffeine"` |
| `rate-limit.max-bucket-count` | `int` | `10000` |
| `rate-limit.bucket-ttl-seconds` | `int` | `3600` |
| `rate-limit.rules-cache-ttl-seconds` | `int` | `30` |
| `rate-limit.track-violations` | `boolean` | `true` |
| `rate-limit.whitelisted-ips` | `List<String>` | `[]` |
| `rate-limit.excluded-patterns` | `List<String>` | actuator, swagger patterns |
| `features.admin.rate-limits` | `boolean` | `true` |

## Package Structure

```
ua.kpi.sc.ratelimit
├── actuator/
│   └── RateLimitInfoContributor.java
├── config/
│   ├── RateLimitCacheConfig.java
│   ├── RateLimitFilterRegistration.java
│   └── RateLimitProperties.java
├── controller/
│   └── RateLimitAdminController.java
├── dto/
│   ├── request/
│   │   ├── CreateRateLimitRuleRequest.java
│   │   ├── UpdateRateLimitRuleRequest.java
│   │   └── RateLimitToggleRequest.java
│   └── response/
│       ├── RateLimitRuleResponse.java
│       └── RateLimitStatsResponse.java
├── entity/
│   ├── RateLimitRule.java
│   └── RateLimitScope.java
├── filter/
│   └── RateLimitFilter.java
├── repository/
│   └── RateLimitRuleRepository.java
├── service/
│   ├── RateLimitService.java
│   └── RateLimitEvaluationService.java
└── storage/
    ├── BucketStore.java
    └── CaffeineBucketStore.java
```
