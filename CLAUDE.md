# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
./gradlew clean build          # Linux/Mac
gradlew.bat clean build        # Windows

# Run
./gradlew bootRun              # Development server (port 8080)

# Run tests
./gradlew test                 # All tests (uses H2 in-memory database)

# Run single test class
./gradlew test --tests "com.scit.soragodong.SoragodongApplicationTests"
```

## Required Environment Variables

- `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` - MySQL connection
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` - Redis for session/cache
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` - Email (Gmail SMTP)
- `JWT_SECRET` - JWT signing key (min 32 characters)
- `GOOGLE_MAPS_API_KEY` - Google Maps API (used on signup page)
- `UPLOAD_PATH` - File upload directory (default: `/upload`)
- `SPRING_PROFILES_ACTIVE` - Profile selection (`local`, `prod`, `test`)

## Architecture Overview

**Spring Boot 3.5 / Java 21** application with layered architecture:

```
controller/ → service/ → repository/ → domain/entity/
```

### Key Patterns

**API Response Standard**: All REST endpoints return `ApiResponse<T>` wrapper:
```java
ApiResponse.success(data)           // 200 with data
ApiResponse.success(message, data)  // 200 with custom message
ApiResponse.error(ErrorCode.XXX)    // Error with ErrorCode enum
```

**Exception Handling**: Throw `CustomException` with `ErrorCode` enum — handled globally by `GlobalExceptionHandler`:
```java
throw new CustomException(ErrorCode.USER_NOT_FOUND);
```

**Entity Base Class**: All entities extend `BaseEntity` for automatic `createdAt`/`updatedAt` timestamps via JPA auditing.

**JPA Naming Strategy**: Uses `PhysicalNamingStrategyStandardImpl` — DB column names must match Java field names exactly (no automatic camelCase→snake_case conversion).

**`open-in-view: false`**: Lazy-loaded associations must be fetched within the service transaction; avoid accessing uninitialized collections in controllers.

### Module Structure

| Module | Description |
|--------|-------------|
| `auth/` | Login, signup, email verification, password reset |
| `community/` | Board posts, replies, likes |
| `finance/` | Financial records and monthly budget tracking |
| `usedmarket/` | Used goods marketplace with keyword notifications |
| `timesale/` | Store products, orders, stock history |

### Domain Enums

| Enum | Values |
|------|--------|
| `UserRole` | `USER`, `ADMIN` |
| `BoardCategory` | Post categories for community board |
| `UsedState` | `SELLING`, `RESERVED`, `SOLD` |
| `FileRefType` | `BOARD`, `PROFILE`, `USED` |
| `NotificationType` | `KEYWORD`, `ORDER`, `COMMENT` |

### Security Configuration

- Spring Security with form login to `/auth/login-proc`
- Login parameter name: `userEmail` (not `username`)
- Session stored in Redis (`spring-session-data-redis`)
- BCrypt password encoding
- Public paths: `/`, `/landing`, `/auth/**`, static resources
- Custom `CustomAuthenticationSuccessHandler` / `CustomAuthenticationFailureHandler`

### Real-time Features

- **WebSocket** (`/ws`): Real-time chat messaging
- **SSE** (`/notifications/subscribe/{userId}`): Push notifications via `SseService`; 1-hour connection timeout

### Database

- **Production**: MySQL 8.0 with `ddl-auto: validate`
- **Local (`application-local.yml`)**: `ddl-auto: update`, Thymeleaf caching disabled
- **Tests**: H2 in-memory with `ddl-auto: create-drop`, activated via `@ActiveProfiles("test")`

### AI Integration

Spring AI with Ollama (`spring-ai-starter-model-ollama`) is included as a dependency. Configure the Ollama endpoint as needed via Spring AI properties.

### File Uploads

Max 10MB per file, 20MB per request. Files are stored via `FileService`/`FileUploadUtil` and referenced by `FileGrp` + `File` entities with a `FileRefType` enum linking files to their owner entity.

### Cross-cutting Concerns

**AOP Logging** (`LoggingAspect`): Automatically logs method entry/exit and execution time for all controllers (INFO) and services (DEBUG). No manual logging boilerplate needed in these layers.

## Code Conventions

- Lombok: `@RequiredArgsConstructor`, `@Getter`, `@Builder`, `@Slf4j`
- DTOs are Java records (immutable)
- `@Async` is enabled on the main class; use for email sending and background tasks
- `ErrorCode` enum messages are in Korean (user-facing)
- Pagination uses `PageResponse<T>` wrapper
