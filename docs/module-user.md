# module-user

User management module. Handles user CRUD, profile updates, password changes, tier management, status management, and partner level assignments.

## Dependencies

| Dependency | Scope |
|---|---|
| `common` | Implementation (project) |
| `spring-boot-starter-data-jpa` | Implementation |
| `spring-boot-starter-test` | Test |

## Components

### UserController

Registered at `/api/v1/users`. Feature flag: `users.management`. Swagger tag: **Users**.

| Method | Endpoint | Tier | MFA | Purpose |
|---|---|---|---|---|
| GET | `/` | SENIOR | No | List users with pagination & filtering |
| GET | `/me` | BASIC | No | Get current user profile |
| PATCH | `/me` | BASIC | No | Update own profile (firstName, lastName) |
| PATCH | `/me/password` | BASIC | No | Change own password |
| GET | `/{id}` | SENIOR | No | Get user by ID |
| POST | `/` | ADMIN | Yes | Create new user |
| PATCH | `/{id}` | ADMIN | No | Update user profile |
| PATCH | `/{id}/tier` | ADMIN | Yes | Change user capability tier |
| PATCH | `/{id}/status` | ADMIN | Yes | Activate/deactivate user |
| DELETE | `/{id}` | ADMIN | Yes | Soft-delete user |
| POST | `/{id}/partners` | ADMIN | Yes | Assign partner level (feature flag: `users.partner-management`) |
| DELETE | `/{id}/partners/{partnerId}` | ADMIN | No | Remove partner level (feature flag: `users.partner-management`) |

### UserService

Core business logic (10 public methods):

- `listUsers(keyword, tier, active, pageable)` — Paginated listing with JPA Specification filters
- `getUserById(id)` — Full user details including partner roles
- `createUser(request, requester)` — Create with audit event
- `updateUser(id, request, requester)` — Update name, audit old→new values
- `updateTier(id, request, requester)` — Change tier, audit + notification
- `updateStatus(id, request, requester)` — Activate/deactivate, audit + notification
- `deleteUser(id, requester)` — Soft-delete (set active=false), audit
- `assignPartnerLevel(userId, request, requester)` — Assign/update partner level, audit + notification
- `changePassword(request, principal)` — Validate current password, update hash, audit
- `removePartnerLevel(userId, partnerId, requester)` — Remove partner level, audit + notification

### Entities

| Entity | Key Fields |
|---|---|
| `User` | id (UUID), email, passwordHash, firstName, lastName, capabilityTier (CapabilityTier), active (boolean), createdAt, updatedAt |
| `PartnerMember` | id (UUID), userId, partnerId, level (PartnerLevel), assignedAt, assignedBy |

### Repositories

| Repository | Description |
|---|---|
| `UserRepository` | `findByEmail`, `existsByEmail`, `updatePasswordById` (modifying query), `JpaSpecificationExecutor` |
| `PartnerMemberRepository` | `findByUserId`, `findByUserIdAndPartnerId`, `findByPartnerId`, `deleteByUserIdAndPartnerId` |

### UserSpecifications

Composable JPA specifications: `searchByKeyword` (case-insensitive on email/firstName/lastName), `hasTier`, `isActive`.

### Adapters

| Adapter | Port | Description |
|---|---|---|
| `UserDetailsAdapter` | `UserDetailsPort` (common) | Loads user principals, creates users, updates passwords/tiers/status, manages partner levels |
| `UserQueryAdapter` | `UserQueryPort` (common) | Queries active user IDs by minimum tier, all active user IDs, email by ID |

### DTOs

| DTO | Type | Description |
|---|---|---|
| `UserResponse` | Response | id, email, firstName, lastName, capabilityTier, active, createdAt, partnerRoles (List\<PartnerMemberResponse\>) |
| `UserListResponse` | Response | id, email, firstName, lastName, capabilityTier, active |
| `PartnerMemberResponse` | Response | partnerId, level, assignedAt |
| `CreateUserRequest` | Request | email, password, firstName, lastName, tier |
| `UpdateUserRequest` | Request | firstName, lastName |
| `ChangePasswordRequest` | Request | currentPassword, newPassword |
| `UpdateTierRequest` | Request | tier |
| `UpdateStatusRequest` | Request | active |
| `AssignPartnerLevelRequest` | Request | partnerId, level |

## Package Structure

```
ua.kpi.sc.user
├── adapter/
│   ├── UserDetailsAdapter.java
│   └── UserQueryAdapter.java
├── controller/
│   └── UserController.java
├── dto/
│   ├── request/
│   │   ├── CreateUserRequest.java
│   │   ├── UpdateUserRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── UpdateTierRequest.java
│   │   ├── UpdateStatusRequest.java
│   │   └── AssignPartnerLevelRequest.java
│   └── response/
│       ├── UserResponse.java
│       ├── UserListResponse.java
│       └── PartnerMemberResponse.java
├── entity/
│   ├── User.java
│   └── PartnerMember.java
├── repository/
│   ├── UserRepository.java
│   ├── PartnerMemberRepository.java
│   └── UserSpecifications.java
└── service/
    └── UserService.java
```
