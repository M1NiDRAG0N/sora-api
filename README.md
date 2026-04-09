# 소라고동 (Sora Godong)

> 가계부 · 중고거래 · 커뮤니티 · 타임세일을 하나로 묶은 **AI 통합 생활 플랫폼**

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-1.1.2-6DB33F?logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Session%2FCache-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?logo=githubactions&logoColor=white)
![k6](https://img.shields.io/badge/Load_Test-k6-7D64FF?logo=k6&logoColor=white)

---

## 소개

소라고동은 동네 생활에 필요한 4가지 핵심 기능(가계부, 중고거래, 커뮤니티, 타임세일)을 제공하며,
AI 도우미 '소라고동'이 자연어 명령만으로 모든 기능을 직접 실행해주는 스마트 생활 플랫폼입니다.

> **예시**: "편의점에서 라면 3000원 지출 기록해줘" → AI가 자동으로 가계부에 저장
> **예시**: "근처에 치킨 타임세일 어디야?" → 거리순으로 정렬된 가게 목록 + 바로가기 링크 반환

---

## 스크린샷

> [스크린샷 추가 필요]

---

## 주요 기능

- **AI 도우미** — 자연어 명령으로 가계부·중고거래·커뮤니티·타임세일 기능을 직접 실행 (Function Calling)
- **가계부** — 수입/지출 등록, 카테고리별 Chart.js 시각화, 월별 예산 관리
- **중고거래** — 물품 등록·수정·삭제, 찜(좋아요), 키워드 알림, 위치 기반 거래
- **커뮤니티** — 게시글 CRUD, 댓글, 좋아요, 무한스크롤, 카테고리·키워드 검색
- **타임세일** — Google Maps 기반 할인 가게 조회, 상품 예약 (원자적 UPDATE로 동시성 제어)
- **실시간 알림** — SSE 기반 키워드 매칭·상태 변경·댓글 알림 (트랜잭션 커밋 후 비동기 발송)
- **실시간 채팅** — WebSocket (STOMP over SockJS) 기반 채팅방
- **인증** — 이메일 인증, BCrypt 암호화, Redis 세션, Google Maps 주소 설정

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Backend** | Spring Boot 3.5, Java 21, Spring Security, Spring AI 1.1.2 |
| **AI** | Groq API (llama-3.3-70b-versatile), Spring AI Function Calling |
| **DB** | MySQL 8.0 (prod), H2 (test) |
| **캐싱/세션** | Redis (Spring Session, Chat History, 조회수) |
| **실시간** | SSE (알림), WebSocket (채팅), Flux SSE (AI 스트리밍) |
| **이메일** | Gmail SMTP (JavaMailSender, @Async) |
| **지도** | Google Maps API |
| **Frontend** | Thymeleaf, Vanilla JS, CSS3, Chart.js, Ionicons |
| **배포** | Docker Compose, Nginx, Oracle Cloud (1GB RAM), GitHub Actions CI/CD |

---

## 시작하기

### 필수 요구사항

- Java 21+
- MySQL 8.0
- Redis
- Groq API Key

### 환경변수 설정

`.env` 파일을 프로젝트 루트에 생성합니다.

```env
# Database
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# Email (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your_app_password

# JWT
JWT_SECRET=your_jwt_secret_min_32_characters

# Google Maps
GOOGLE_MAPS_API_KEY=your_google_maps_api_key

# File Upload
UPLOAD_PATH=/upload

# Spring Profile
SPRING_PROFILES_ACTIVE=local
```

로컬 개발 시 `application-local.yml`에 Groq API 키를 추가합니다.

### 설치 및 실행

```bash
# 저장소 클론
git clone https://github.com/M1NiDRAG0N/sora-api.git
cd sora-api

# 빌드 (Windows)
gradlew.bat clean build -x test

# 빌드 (Linux/Mac)
./gradlew clean build -x test

# 개발 서버 실행 (port 8080)
gradlew.bat bootRun      # Windows
./gradlew bootRun         # Linux/Mac
```

접속: `http://localhost:8080`

### 테스트 실행

```bash
# 전체 테스트 (H2 인메모리 DB 자동 사용)
gradlew.bat test

# 단일 테스트 클래스
gradlew.bat test --tests "com.scit.soragodong.SoragodongApplicationTests"
```

---

## 배포 (CI/CD)

`main` 브랜치에 push 하면 자동으로 배포됩니다.

```
push to main
  → GitHub Actions (Gradle build + Docker image build)
  → GHCR (GitHub Container Registry) push
  → Oracle Cloud 서버 SSH 배포
  → Docker Compose restart (nginx:80 + sora-api:8080)
```

**컨테이너 설정:**
- `mem_limit: 768m`, JVM 힙: `MaxRAMPercentage=55.0` (~422m)
- `MaxMetaspaceSize=192m`, `UseG1GC`, `ExitOnOutOfMemoryError`

---

## 프로젝트 구조

```
sora-api/
├── src/main/java/com/scit/soragodong/
│   ├── ai/                               # AI Function Calling Tools
│   │   ├── FinanceTool.java
│   │   ├── UsedMarketTool.java
│   │   ├── CommunityTool.java
│   │   └── TimesaleTool.java
│   ├── aspect/
│   │   └── LoggingAspect.java            # AOP 자동 로깅 (컨트롤러/서비스)
│   ├── config/                           # Security, Redis, WebSocket, WebMvc 설정
│   ├── controller/                       # REST + View 컨트롤러
│   ├── domain/
│   │   ├── dto/                          # Java Record DTOs
│   │   ├── entity/                       # JPA 엔티티
│   │   ├── enums/                        # UserRole, UsedState, BoardCategory 등
│   │   └── response/                     # ApiResponse<T>, PageResponse<T>
│   ├── exception/                        # CustomException, ErrorCode, GlobalExceptionHandler
│   ├── repository/                       # Spring Data JPA Repositories
│   ├── service/                          # 비즈니스 로직
│   └── util/                             # 공통 유틸
└── src/main/resources/
    ├── application.yml                   # 공통 설정 (환경변수 기반)
    ├── application-local.yml             # 로컬 개발 설정
    └── templates/                        # Thymeleaf 템플릿
        ├── auth/                         # 로그인, 회원가입, 계정찾기
        ├── finance/
        ├── usedmarket/
        ├── community/
        ├── timesale/
        ├── chat/                         # AI 채팅, 채팅방
        ├── notification/
        └── profile/
```

---

## 도메인 모델

```
Users ──< Finance
      ──< MonthlyBudget
      ──< Board ──< BoardReply
                ──< LikeCount
      ──< Used ──< UsedLike
               ──< UsedKeyword (알림 키워드)
      ──< UserOrder ──> StoreProduct ──> Store
      ──< Notification
FileGrp ──< File  (BOARD / PROFILE / USED)
```

---

## AI 도우미 - 소라고동

Groq API (llama-3.3-70b-versatile) + Spring AI SSE 스트리밍으로 자연어 명령을 실제 앱 기능으로 실행합니다.

| Tool | 실행 가능한 기능 |
|------|----------------|
| `FinanceTool` | 가계부 등록, 목록 조회, 월별 요약, 예산 설정, 삭제 |
| `UsedMarketTool` | 물품 등록·삭제·상태변경, 키워드 등록·삭제·조회, 목록 검색 |
| `CommunityTool` | 게시글 작성·조회·삭제 |
| `TimesaleTool` | 키워드로 주변 상품 검색 (거리순 정렬 + 바로가기 링크), 상품 예약 |

**사용 예시:**
```
"편의점에서 라면 삼각김밥 3000원 지출 가계부 기록해줘"
→ recordFinance() 호출 → DB 저장

"아이폰 충전기 15000원에 중고 등록해줘"
→ registerUsed() 호출 → DB 저장

"이번달 가계부 요약해줘"
→ summarizeMonth() 호출 → 수입/지출 합계 반환

"근처에 치킨 타임세일 어디야?"
→ searchTimesaleByKeyword("치킨") → 거리순 가게 목록 + 클릭 가능한 링크 반환
```

---

## 주요 설계 결정

| 결정 | 이유 |
|------|------|
| 모든 REST API → `ApiResponse<T>` 래퍼 반환 | 빈 body로 인한 `Unexpected end of JSON input` 방지 |
| `@Transactional` Controller 클래스 수준 금지 | Thymeleaf 렌더링 중 DB 커넥션 점유로 인한 풀 고갈 방지 |
| HikariCP `max-lifetime=1800000` (30분) | MySQL `wait_timeout`보다 짧으면 커넥션 단절 발생 |
| 조회수 중복 방지: Redis Set + 24시간 TTL | DB 없이 24시간 기준 중복 방지 |
| 트랜잭션 커밋 후 알림: `TransactionSynchronization` | 롤백 시 알림 발송 방지 |
| AI Tool: 요청마다 POJO 인스턴스 생성 | userIdx를 가진 Thread-safe 도구 보장 |
| Java 21 Virtual Thread Executor (SSE) | `SimpleAsyncTaskExecutor` 경고 제거 + 효율적 스트리밍 |
| `reserveProduct` Controller `@Transactional` 제거 | 서비스 rollback-only → `UnexpectedRollbackException` → 500 방지 |
| 예약 시 `JOIN FETCH`로 store 조회 통합 | 트랜잭션당 DB 쿼리 5→3회, avg 응답시간 63% 개선 |

---

## 성능 개선 & 트러블슈팅 (k6 부하테스트)

배포 후 k6로 실제 트래픽을 시뮬레이션해 두 가지 버그를 발견하고 수정했습니다.

### 테스트 환경

| 항목 | 내용 |
|------|------|
| 도구 | k6 |
| 시나리오 | 커뮤니티 탐색 (최대 40 VU) + 타임세일 조회 (8 VU) + 타임세일 동시 예약 스파이크 (20 req/s) |
| 모니터링 | `/api/internal/stats` (CPU, 메모리, HikariCP 풀 실시간 수집) |

---

### [버그 1] Controller `@Transactional` 이중 적용 → 동시 예약 500 에러 99.3%

**증상**

k6로 초당 20건 예약 스파이크 테스트 시 `http_req_failed 99.3%`, 서버 로그에 `UnexpectedRollbackException` 다수 발생.

**원인**

```
TimeSaleController.reserveProduct()   ← @Transactional (컨트롤러)
  └─ TimesaleService.reserveProduct() ← @Transactional (서비스)
       └─ 재고 부족 → IllegalStateException 발생
            → 서비스 트랜잭션: rollback-only 마킹
       컨트롤러가 catch → 409 반환 시도
       컨트롤러 트랜잭션 커밋 → rollback-only 감지 → UnexpectedRollbackException
       GlobalExceptionHandler → 500
```

컨트롤러가 예외를 catch해 409를 반환해도, 상위 트랜잭션이 커밋 시점에 `rollback-only` 상태를 감지해 `UnexpectedRollbackException`을 던집니다. `@Transactional`은 서비스 계층에만 위치해야 합니다.

**수정**

```java
// Before
@PostMapping("/reserve")
@Transactional  // ← 제거
public ResponseEntity<?> reserveProduct(...) { ... }

// After
@PostMapping("/reserve")
public ResponseEntity<?> reserveProduct(...) { ... }
```

**결과:** 500 에러 99.3% → **0%**

---

### [버그 2] 예약당 불필요한 DB 쿼리 → avg 응답시간 2.85s

**증상**

500 에러를 수정한 뒤에도 동시 예약 avg 응답시간 2.85s, 중앙값 3.41s로 병목 지속.

**원인**

예약 1건 처리 시 트랜잭션 내 DB 쿼리가 5번 발생했습니다.

```
1. findById(productNum)          ← SELECT (상품)
2. if (stock < quantity) throw   ← 메모리 재고 확인 (중복)
3. findById(storeIdx)            ← SELECT (가게) ← 불필요
4. decreaseStock() UPDATE        ← WHERE quantity >= :quantity (원자적)
5. save(UserOrder)               ← INSERT
6. save(ProductStockHistory)     ← INSERT
```

`StoreProduct`가 이미 `@ManyToOne Store`를 가지므로 가게를 별도로 조회할 필요가 없었습니다.
또한 `decreaseStock()`이 `WHERE productQuantity >= :quantity` 조건으로 원자적으로 처리하므로 메모리 재고 확인도 중복이었습니다.

**수정**

```java
// Repository: JOIN FETCH로 상품+가게 한 번에 조회
@Query("SELECT p FROM StoreProduct p JOIN FETCH p.store WHERE p.productNum = :productNum")
Optional<StoreProduct> findByIdWithStore(@Param("productNum") Integer productNum);

// Service: 중복 재고 확인 제거 + store는 product에서 직접 참조
StoreProduct product = storeProductRepository.findByIdWithStore(productNum)...;
int updatedRows = storeProductRepository.decreaseStock(productNum, quantity); // 원자적 처리
if (updatedRows == 0) throw new IllegalStateException("재고가 부족합니다.");
Store store = product.getStore(); // 별도 SELECT 없음
```

**결과**

| 지표 | 수정 전 | 수정 후 | 개선 |
|------|---------|---------|------|
| avg 응답시간 | 2.85s | **1.06s** | ↓ 63% |
| 중앙값 | 3.41s | **0.63s** | ↓ 82% |
| 서버 에러(5xx) | 430건 | **0건** | - |

---

### 최종 복합 테스트 결과

커뮤니티 탐색 (최대 40 VU) + 타임세일 조회 (8 VU) 혼합 트래픽 6분 지속.

| 지표 | 결과 |
|------|------|
| p(50) | 45ms |
| p(95) | 501ms |
| p(99) | 1.18s |
| 서버 에러율 | **0.00%** |
| 전체 체크 통과율 | **100%** |

---

## 라이센스

이 프로젝트는 학습/포트폴리오 목적으로 제작되었습니다.
