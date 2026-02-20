# Teamflow Backend (Portfolio Project)

Backend demo project built with “production-minded” practices: layered architecture, clean domain model, JWT security, Flyway migrations, consistent error responses (ProblemDetail), and strong test coverage (unit + integration with Testcontainers).

Conceptually it’s a lightweight B2B team service:
**Workspaces (organizations) → Projects → Tasks**, with membership-based access and invite flow via email tokens.

---

## Tech Stack

- Java 21, Spring Boot 3.5.x
- Spring Web, Spring Security (stateless)
- JWT (jjwt)
- Spring Data JPA + PostgreSQL
- Flyway migrations
- MapStruct
- OpenAPI/Swagger (springdoc)
- Testing:
    - Unit: JUnit 5 + Mockito
    - Integration: MockMvc + Testcontainers PostgreSQL

---

## Key Features

### Authentication & Users
- Register → user created in `PENDING` state.
- Email verification → user becomes `ACTIVE`.
- Login allowed **only** for `ACTIVE` users.
- JWT access token (`Bearer`) returned on login.

### Workspaces & Membership
- Creating a workspace automatically creates an `OWNER` membership for the creator.
- Access to a workspace is **membership-aware**:
    - If user is not a member, API returns **404 Not Found** (instead of 403),
      so the system does not reveal the existence of resources.

### Workspace Invites
- Workspace owner can invite a user by email.
- Invite token is generated as raw token, but stored as **hash** in DB.
- Accept invite:
    - token must be valid and not expired
    - current user email must match the invite email
    - membership is created on accept

### Projects & Tasks
- Projects exist inside a workspace.
- Tasks exist inside a project.
- Workspace membership gates access to projects and tasks.

### Consistent Errors (ProblemDetail)
All errors are returned in a unified structure using `ProblemDetail`
with meaningful `status`, `title`, and `detail`.

### Deletion policy (soft-delete)

For demo purposes the API avoids physical deletes for core entities (workspaces, projects, tasks).
Instead, records are kept and their lifecycle is managed via statuses (e.g., archived/closed), so history can be preserved and audited.

---

## API Overview

Base URL: `/api/v1`

### Auth
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/verify-email`
- `POST /api/v1/auth/login`

### Users
- `GET /api/v1/users/me`

### Workspaces
- `GET /api/v1/workspaces`
- `POST /api/v1/workspaces`
- `GET /api/v1/workspaces/{id}`
- `PATCH /api/v1/workspaces/{id}`
- `POST /api/v1/workspaces/{id}/close`
- `POST /api/v1/workspaces/{id}/restore`
- `GET /api/v1/workspaces/closed`

Membership:
- `GET /api/v1/workspaces/{id}/members`
- `DELETE /api/v1/workspaces/{id}/leave`
- `DELETE /api/v1/workspaces/{id}/members/{userId}`
- `POST /api/v1/workspaces/{id}/members/{userId}/promote`

Invites:
- `POST /api/v1/workspaces/invites/{workspaceId}`
- `POST /api/v1/workspaces/invites/accept`

### Projects (within workspace)
- `GET /api/v1/workspaces/{workspaceId}/projects`
- `POST /api/v1/workspaces/{workspaceId}/projects`
- `GET /api/v1/workspaces/{workspaceId}/projects/{id}`
- `PATCH /api/v1/workspaces/{workspaceId}/projects/{id}`
- `POST /api/v1/workspaces/{workspaceId}/projects/{id}/archive`
- `POST /api/v1/workspaces/{workspaceId}/projects/{id}/restore`

### Tasks (within project)
- `GET /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks`
- `POST /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks`
- `GET /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}`
- `PATCH /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}`
- `POST /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/status`
- `POST /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/assign`
- `POST /api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/unassign`

### Health
- `GET /api/v1/health`

---

## Security Notes

### JWT Secret (Dev vs Production)
In `application.properties`:

```properties
security.jwt.secret=${JWT_SECRET:V8n#kL29sd8dL29sd8dL29sd8dL29sd8d}
```

This fallback value is only for local development/demo.

In real production setup, the secret must be provided via environment variable JWT_SECRET.

### Stateless Auth

- No server sessions (`SessionCreationPolicy.STATELESS`)
- All protected endpoints require `Authorization: Bearer <accessToken>`

---

## Email Verification & Workspace Invites (Demo Mode)

This project uses an abstraction `VerificationNotifier` for delivering tokens:

- `sendEmailVerification(email, rawToken)` for email verification
- `sendWorkspaceInvite(email, rawToken, workspaceId, expiresAt)` for workspace invites

In this demo version, a `LogVerificationNotifier` can log tokens instead of sending real emails.
In tests, `TestVerificationNotifier` captures the last tokens in memory so integration tests can complete full flows.

---

## Running Locally

### Prerequisites
- Java 21
- Docker + Docker Compose (for Postgres)

### Start PostgreSQL
```bash
docker compose up -d db
```

### Run the backend (Gradle)
```bash
./gradlew bootRun
```

Backend will start on:
- `http://localhost:8080`

---

## Running with Docker (App + DB)

If your `docker-compose.yml` includes an `app` service, you can run everything with:

```bash
docker compose up --build
```

---

## Configuration

### application.properties (local defaults)

Key properties:

- DB connection can be overridden by environment variables:
    - `DB_URL`
    - `DB_USER`
    - `DB_PASSWORD`

- JWT:
    - `JWT_SECRET` (required in production)
    - `JWT_TTL_SECONDS` (default 3600)
    - `JWT_ISSUER` (default `teamflow-backend`)

Example snippet:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/teamflow}
spring.datasource.username=${DB_USER:teamflow}
spring.datasource.password=${DB_PASSWORD:teamflow}

security.jwt.secret=${JWT_SECRET:V8n#kL29sd8dL29sd8dL29sd8dL29sd8d}
security.jwt.access-token-ttl-seconds=${JWT_TTL_SECONDS:3600}
security.jwt.issuer=${JWT_ISSUER:teamflow-backend}
```

---

## Swagger / OpenAPI

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui/index.html`

---

## Database Migrations (Flyway)

Flyway runs automatically on application startup.

Migration scripts are located at:
- `src/main/resources/db/migration`

Hibernate schema is set to validate mode:

- `spring.jpa.hibernate.ddl-auto=validate`

This means:
- Flyway owns schema changes
- Hibernate validates that entities match the schema

---

## Testing

### Unit + Integration tests

Integration tests use:
- Testcontainers PostgreSQL
- MockMvc
- Real Spring Boot context

Run all tests:

```bash
./gradlew test
```

---

## Error Handling (ProblemDetail)

All errors are returned using Spring `ProblemDetail` in a consistent format.

Example:

```json
{
"status": 404,
"title": "Not Found",
"detail": "Workspace not found."
}
```

---

## Design Decisions

### 404 for non-members (workspace-scoped resources)

For workspace-scoped resources (workspace/projects/tasks), if the current user is not a workspace member,
the API returns **404 Not Found** instead of **403 Forbidden**.

Reason:
- it prevents leaking resource existence in multi-tenant systems
- common B2B/SaaS security pattern

### Token storage as hash

Email verification tokens and workspace invite tokens are stored as **hashes**, not raw values.

Reason:
- reduces impact of database leakage
- same principle as storing password hashes

### Stateless security

- no server sessions
- JWT in `Authorization: Bearer <token>`
- services read authenticated identity via `CurrentUserProvider` (no direct dependency on HTTP layer)

---

## Architecture

Layered structure:

- API layer (`...api`): controllers, DTOs, request/response models
- Service layer (`...service`): business logic, access checks, transactions
- Domain layer (`...domain`): entities and invariants
- Repository layer (`...repo`): persistence and queries
- Security (`...security`): JWT filter, security config, helpers
- Common (`...common`): shared utilities (errors, pagination, etc.)

Controllers stay thin; business rules live in services.

---

## Frontend 

Frontend is currently in development and will be in separate repository.