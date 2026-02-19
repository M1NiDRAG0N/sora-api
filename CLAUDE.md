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

The application requires these environment variables (set in IDE or `.env`):
- `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` - MySQL connection
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` - Redis for session/cache
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` - Email (Gmail SMTP)
- `JWT_SECRET` - JWT signing key (min 32 characters)
- `GOOGLE_MAPS_API_KEY` - Google Maps API
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

**Exception Handling**: Throw `CustomException` with `ErrorCode` enum - handled globally by `GlobalExceptionHandler`:
```java
throw new CustomException(ErrorCode.USER_NOT_FOUND);
```

**Entity Base Class**: Entities extend `BaseEntity` for automatic `createdAt`/`updatedAt` timestamps.

### Module Structure

| Module | Description |
|--------|-------------|
| `auth/` | Login, signup, email verification, password reset |
| `community/` | Board posts, replies, likes |
| `finance/` | Financial records management |
| `usedmarket/` | Used goods marketplace with keyword notifications |
| `timesale/` | Store products, orders, stock history |

### Security Configuration

- Spring Security with form login (`/auth/login-proc`)
- Session storage in Redis
- BCrypt password encoding
- Login parameter: `userEmail` (not `username`)
- Public paths: `/`, `/landing`, `/auth/**`, static resources

### Real-time Features

- **WebSocket**: Real-time chat messaging
- **SSE (Server-Sent Events)**: Push notifications via `SseService`

### Database

- **Production**: MySQL 8.0 with `ddl-auto: validate`
- **Tests**: H2 in-memory with `ddl-auto: create-drop`
- Test profile activated via `@ActiveProfiles("test")`

## Code Conventions

- Lombok annotations for boilerplate (`@RequiredArgsConstructor`, `@Getter`, `@Builder`)
- DTOs use Java records (e.g., `UserDto`)
- Async operations with `@Async` (enabled in main class)
- Korean language for user-facing messages in `ErrorCode` enum
