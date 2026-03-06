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
- `GOOGLE_MAPS_API_KEY` - Google Maps API (used on signup and timesale pages)
- `UPLOAD_PATH` - File upload directory (default: `/upload`)
- `SPRING_PROFILES_ACTIVE` - Profile selection (`local`, `prod`, `test`)

## Deployment

- **CI/CD**: GitHub Actions (`.github/workflows/deploy.yml`) — push to `main` triggers Gradle build → Docker image push to GHCR → SSH deploy
- **Runtime**: Docker Compose on server — `nginx` (port 80) + `sora-api` (port 8080 internal)
- **Container limits**: `mem_limit: 768m`, `restart: unless-stopped`
- **JVM flags** (Dockerfile): `MaxRAMPercentage=55.0` (~422m heap), `MaxMetaspaceSize=192m`, `MetaspaceSize=96m`, `UseG1GC`, `ExitOnOutOfMemoryError`

## Architecture Overview

**Spring Boot 3.5 / Java 21** application with layered architecture:

```
controller/ → service/ → repository/ → domain/entity/
```

### Key Patterns

**API Response Standard**: All REST endpoints must return `ApiResponse<T>` wrapper (never `void`):
```java
ApiResponse.success(data)           // 200 with data
ApiResponse.success(message, data)  // 200 with custom message
ApiResponse.error(ErrorCode.XXX)    // Error with ErrorCode enum
```
> **Why**: `void` endpoints return empty HTTP body. Frontend `api.js handleResponse()` calls
> `response.text()` first; an empty body would silently return `null`. Never return bare `void`
> from a `@ResponseBody` method.

**Exception Handling**: Throw `CustomException` with `ErrorCode` enum — handled globally by `GlobalExceptionHandler`:
```java
throw new CustomException(ErrorCode.USER_NOT_FOUND);
```

**Entity Base Class**: All entities extend `BaseEntity` for automatic `createdAt`/`updatedAt` timestamps via JPA auditing.

**JPA Naming Strategy**: Uses `PhysicalNamingStrategyStandardImpl` — DB column names must match Java field names exactly (no automatic camelCase→snake_case conversion).

**`open-in-view: false`**: Lazy-loaded associations must be fetched within the service transaction; avoid accessing uninitialized collections in controllers.

**@Transactional placement**: NEVER put `@Transactional` at Controller class level. It holds a DB
connection for the entire request including Thymeleaf rendering, exhausting the connection pool.
Only annotate specific service methods that need transactions.

### Module Structure

| Module | Description |
|--------|-------------|
| `auth/` | Login, signup, email verification, password reset |
| `community/` | Board posts, replies, likes |
| `finance/` | Financial records and monthly budget tracking |
| `usedmarket/` | Used goods marketplace with keyword notifications |
| `timesale/` | Store products, orders, stock history |
| `ai/` | AI chat service, streaming, tool definitions |

### Domain Enums

| Enum | Values |
|------|--------|
| `UserRole` | `USER`, `ADMIN` |
| `BoardCategory` | Post categories for community board |
| `UsedState` | `SELLING`, `RESERVED`, `SOLD` |
| `FileRefType` | `BOARD`, `PROFILE`, `USED` |
| `NotificationType` | `KEYWORD`, `ORDER`, `COMMENT` |
| `FinanceCategory` | `SALARY`, `FOOD`, `TRANSPORT`, `SHOPPING`, `EVENT`, `SAVINGS`, `ETC` — Korean labels + AI keyword arrays + `fromAI(String)` factory |

### Security Configuration

- Spring Security with form login to `/auth/login-proc`
- Login parameter name: `userEmail` (not `username`)
- Session stored in Redis (`spring-session-data-redis`)
- BCrypt password encoding
- Public paths: `/`, `/landing`, `/auth/**`, static resources
- Custom `CustomAuthenticationSuccessHandler` / `CustomAuthenticationFailureHandler`

### Real-time Features

- **WebSocket** (`/ws`): Real-time chat messaging (STOMP over SockJS)
- **SSE** (`/notifications/subscribe/{userId}`): Push notifications via `SseService`; 1-hour connection timeout
- **AI Streaming** (`/ai/chat/stream`): `Flux<String>` with `MediaType.TEXT_EVENT_STREAM_VALUE`

### Database

- **Production**: MySQL 8.0 with `ddl-auto: validate`
- **Local (`application-local.yml`)**: `ddl-auto: update`, Thymeleaf caching disabled
- **Tests**: H2 in-memory with `ddl-auto: create-drop`, activated via `@ActiveProfiles("test")`
- **HikariCP**: `maximum-pool-size=20`, `max-lifetime=1800000` (30min), `idle-timeout=600000` (10min)

### AI Integration

Spring AI with Ollama (`spring-ai-starter-model-ollama`). Chat history stored in Redis via `RedisChatMemory`.

**AI Tools** (created per-request in `AiChatService`, not Spring beans):

| Tool Class | Capabilities |
|---|---|
| `FinanceTool` | Record income/expense, query history. Normalizes category via `FinanceCategory.fromAI()` |
| `UsedMarketTool` | Register/list/delete used goods, manage keywords. Has `userLat`/`userLng`/`userAddress` |
| `CommunityTool` | Write posts and replies on community board |
| `TimesaleTool` | Search by keyword (distance-sorted), list stores/products, reserve. Has `userLat`/`userLng` |

**AI System Prompt rules**:
- Language: `[절대 규칙]` — Korean only, zero non-Korean characters
- Timesale: use `searchTimesaleByKeyword` when user asks about food/products they want; pass through `[name](/timesale/detail?storeIdx=N)` links as-is

### File Uploads

Max 10MB per file, 20MB per request. Files are stored via `FileService`/`FileUploadUtil` and referenced by `FileGrp` + `File` entities with a `FileRefType` enum linking files to their owner entity.

`FileController` caches file metadata (`File` entity) in `ConcurrentHashMap` to avoid DB query on every image request.

### Cross-cutting Concerns

**AOP Logging** (`LoggingAspect`): Automatically logs method entry/exit and execution time for all controllers (INFO) and services (DEBUG). No manual logging boilerplate needed in these layers.

**WebMvc Async** (`WebMvcConfig`): `ThreadPoolTaskExecutor` (core=4, max=20) configured for MVC async support (needed for `Flux<String>` AI streaming). Default timeout 90s.

## Code Conventions

- Lombok: `@RequiredArgsConstructor`, `@Getter`, `@Builder`, `@Slf4j`
- DTOs are Java records (immutable)
- `@Async` is enabled on the main class; use for email sending and background tasks
- `ErrorCode` enum messages are in Korean (user-facing)
- Pagination uses `PageResponse<T>` wrapper

## Frontend Conventions

### api.js
All fetch calls go through `API.get/post/put/patch/delete/postFormData`.
- Shows `Utils.showLoading()` before, `Utils.hideLoading()` in `finally`
- `handleResponse`: reads `response.text()` first, parses JSON only if non-empty

### Utils.notify() Toast
Positioned at top-right of `.app-container` using `getBoundingClientRect()`.
Types: `info` / `success` / `warning` / `error`.

### Thymeleaf + JavaScript
**IMPORTANT**: Do NOT use regex literals containing `[(` inside `<script>` tags in Thymeleaf templates.
Thymeleaf parses `[(expr)]` as inline utext expression and throws `TemplateInputException`.
- Use `new RegExp('...')` string constructor instead of `/regex/` literals with `[(`
- Or add `th:inline="none"` to the `<script>` tag to disable all inline processing

### AI Chat (chat-ai.html)
- Streaming via direct `fetch('/ai/chat/stream')` — NOT through `api.js` (response is SSE, not JSON)
- `_formatText(text)`: escapeHtml → `\\n` → `<br>` → `[text](/path)` → `<a class="chat-link">`
- Script tag has `th:inline="none"` to avoid Thymeleaf regex parsing issues

## Known Pitfalls

| Issue | Root Cause | Fix |
|---|---|---|
| Connection pool exhaustion | `@Transactional` on Controller class | Remove from class; add only to service methods that need it |
| HikariCP connections dying | `max-lifetime` too short (was 50s) | Set to 1800000ms (30min) |
| `Unexpected end of JSON input` | `@ResponseBody void` returns empty body | Return `ApiResponse<?>` from all endpoints |
| `TemplateInputException` on `[(` | Thymeleaf parses JS regex as inline utext | Add `th:inline="none"` to script tag |
| Metaspace OOM | No `MaxMetaspaceSize` limit set | Set `-XX:MaxMetaspaceSize=192m` |
| AI responds in Chinese | Weak language rule in system prompt | Use `[절대 규칙]` prefix with explicit exclusion list |
