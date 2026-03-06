# 소라고동 (Sora Godong)

> 가계부 · 중고거래 · 커뮤니티 · 타임세일을 하나로 묶은 **AI 통합 생활 플랫폼**

---

## 프로젝트 개요

소라고동은 동네 생활에 필요한 4가지 핵심 기능(가계부, 중고거래, 커뮤니티, 타임세일)을 제공하며,
AI 도우미 '소라고동'이 자연어 명령만으로 모든 기능을 직접 실행해주는 스마트 생활 앱입니다.

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Backend** | Spring Boot 3.5, Java 21, Spring Security, Spring AI 1.1.2 |
| **AI** | Groq API (llama-3.3-70b-versatile), Spring AI Function Calling |
| **DB** | MySQL 8.0 (prod), H2 (test) |
| **캐싱/세션** | Redis (Spring Session) |
| **실시간** | SSE (알림), WebSocket (채팅) |
| **이메일** | Gmail SMTP (JavaMailSender, @Async) |
| **지도** | Google Maps API |
| **Frontend** | Thymeleaf, Vanilla JS, CSS3, Chart.js, Ionicons |
| **빌드/배포** | Gradle, Oracle Cloud (1GB RAM) |

---

## 주요 기능

### 1. 인증 (Auth)
- 이메일/비밀번호 회원가입 (BCrypt 암호화)
- 이메일 인증 코드 발송 및 검증
- 비밀번호 재설정 (이메일 코드 인증)
- Google Maps 기반 주소/위치 설정 (회원가입 시)
- 닉네임·이메일 실시간 중복 확인
- Redis 세션 기반 로그인 유지

### 2. 가계부 (Finance)
- 수입/지출 내역 등록·수정·삭제
- 카테고리별 분류 (식비, 교통, 생활 등)
- 고정 지출 설정 (isFixed + 기간)
- 월별 예산 설정 및 관리 (MonthlyBudget)
- 달력 뷰 + 카테고리별 Chart.js 시각화
- 월별 수입/지출 합계 요약

### 3. 중고거래 (Used Market)
- 물품 등록·수정·논리삭제 (이미지 첨부 가능)
- 상태 관리: `TRADING` → `SALE` → `RESERVED` → `SOLD`
- 찜(좋아요) 토글 + 찜한 사용자에게 알림
- 키워드 알림: 최대 10개 등록, 신규 물품 등록 시 자동 매칭
- 위치 기반 거래 장소 설정 (위도/경도)
- 페이징 검색 (제목·내용 키워드)
- 24시간 중복 방지 조회수 (Redis)

### 4. 커뮤니티 (Community)
- 게시글 CRUD + 이미지 첨부
- 카테고리: `DISTRIBUTION`(나눔) · `TIP` · `HUMOR` · `COOK` · `QUESTION`
- 댓글 등록·수정·삭제
- 좋아요 토글 (LikeCount 복합키 테이블)
- 무한스크롤 페이징 (10개씩)
- 카테고리·키워드 복합 검색

### 5. 타임세일 (Timesale)
- 이벤트 진행 가게 목록 조회 + Google Maps 지도 표시
- 할인 상품 목록 조회 (eventPrice)
- 가게별 재고 있는 상품 조회
- 상품 예약 처리 (재고 감소 + 주문 이력 저장)
- 동시성 제어 (Pessimistic Lock)

### 6. 실시간 알림 (Notification)
- SSE(Server-Sent Events) 기반 실시간 푸시
- 알림 타입: `KEYWORD`(키워드 매칭) · `USED_STATE_CHANGE`(상태 변경) · `USED_LIKE`(찜) · `ORDER`(주문) · `COMMENT`(댓글)
- 알림 목록 조회·읽음 처리·삭제 (논리 삭제)
- 미읽음 수 배지 표시
- 트랜잭션 커밋 후 비동기 알림 (@Async + TransactionSynchronization)

### 7. 실시간 채팅 (Chat)
- WebSocket 기반 채팅방 구조
- 채팅방 목록 / 채팅방 입장

### 8. AI 도우미 - 소라고동
- Groq API (llama-3.3-70b-versatile) + Spring AI SSE 스트리밍
- **Function Calling**: 자연어 명령으로 앱 기능 직접 실행

| Tool | 실행 가능한 기능 |
|------|----------------|
| `FinanceTool` | 가계부 등록, 목록 조회, 월별 요약, 예산 설정, 삭제 |
| `UsedMarketTool` | 물품 등록·삭제·상태변경, 키워드 등록·삭제·조회, 목록 검색 |
| `CommunityTool` | 게시글 작성·조회·삭제 |
| `TimesaleTool` | 키워드로 주변 맛집/상품 검색 (거리순 정렬 + 페이지 바로가기 링크), 할인 상품 조회, 상품 예약 |

**사용 예시:**
```
"편의점에서 라면 삼각김밥 3000원 지출 가계부 기록해줘"
→ recordFinance() 호출 → DB 저장

"아이폰 충전기 15000원에 중고 등록해줘"
→ registerUsed() 호출 → DB 저장

"이번달 가계부 요약해줘"
→ summarizeMonth() 호출 → 수입/지출 합계 반환

"근처에 치킨 타임세일 어디야?"
→ searchTimesaleByKeyword("치킨") → 거리순 가게 목록 + 클릭 가능한 페이지 링크 반환
```

### 9. 프로필 (Profile)
- 프로필 이미지 업로드/변경
- 닉네임, 주소, 위치 수정

### 10. 파일 업로드 (File)
- 게시글·중고물품·프로필 이미지 업로드
- FileGrp + File 엔티티로 그룹 관리
- 최대 10MB/파일, 20MB/요청

---

## 프로젝트 구조

```
sora-api/
├── src/main/java/com/scit/soragodong/
│   ├── SoragodongApplication.java        # 메인 (@EnableAsync)
│   │
│   ├── ai/                               # AI Function Calling Tools
│   │   ├── FinanceTool.java
│   │   ├── UsedMarketTool.java
│   │   ├── CommunityTool.java
│   │   └── TimesaleTool.java
│   │
│   ├── aspect/
│   │   └── LoggingAspect.java            # AOP 자동 로깅
│   │
│   ├── common/
│   │   └── BaseEntity.java               # createdAt/updatedAt 자동 관리
│   │
│   ├── config/
│   │   ├── SecurityConfig.java           # Spring Security 설정
│   │   ├── RedisConfig.java              # Redis 설정
│   │   ├── SessionConfig.java            # 세션 설정
│   │   ├── WebSocketConfig.java          # WebSocket 설정
│   │   ├── WebMvcConfig.java             # Virtual Thread Executor (SSE)
│   │   └── EmailProperties.java
│   │
│   ├── controller/
│   │   ├── AiChatController.java         # AI 채팅 SSE 스트리밍
│   │   ├── AuthController.java           # 회원가입/로그인/비번찾기
│   │   ├── CommunityController.java      # 커뮤니티 CRUD
│   │   ├── FinanceController.java        # 가계부 CRUD
│   │   ├── NotificationController.java   # SSE 알림 구독/CRUD
│   │   ├── ProfileController.java        # 프로필 관리
│   │   ├── TimeSaleController.java       # 타임세일
│   │   ├── UsedMarketController.java     # 중고거래 CRUD
│   │   ├── FileController.java           # 파일 업로드/조회
│   │   └── HomeController.java / MainController.java
│   │
│   ├── domain/
│   │   ├── dto/                          # Java Record DTOs
│   │   ├── entity/                       # JPA 엔티티
│   │   ├── enums/                        # UserRole, UsedState, BoardCategory 등
│   │   └── response/                     # ApiResponse<T>, PageResponse<T>
│   │
│   ├── exception/
│   │   ├── CustomException.java
│   │   ├── ErrorCode.java                # 에러 코드 enum (한국어 메시지)
│   │   └── GlobalExceptionHandler.java
│   │
│   ├── handler/
│   │   ├── CustomAuthenticationSuccessHandler.java
│   │   └── CustomAuthenticationFailureHandler.java
│   │
│   ├── repository/                       # Spring Data JPA Repositories
│   │
│   ├── security/
│   │   └── CustomUserDetails.java        # 인증 사용자 정보
│   │
│   ├── service/
│   │   ├── AiChatService.java            # Spring AI + Tool 등록
│   │   ├── CommunityService.java
│   │   ├── FinanceService.java
│   │   ├── NotificationService.java      # 알림 발송 (@Async)
│   │   ├── SseService.java               # SSE 연결 관리
│   │   ├── TimesaleService.java
│   │   ├── UsedService.java              # 중고거래 + 키워드 알림
│   │   ├── UserService.java
│   │   ├── EmailService.java
│   │   ├── FileService.java
│   │   ├── ProfileService.java
│   │   └── ViewCountService.java         # Redis 조회수 중복 방지
│   │
│   └── util/
│       ├── ValidationUtil.java
│       ├── FileUploadUtil.java
│       ├── EncryptUtil.java
│       ├── DateTimeUtil.java
│       └── StringUtil.java
│
└── src/main/resources/
    ├── application.yml                   # 공통 설정 (환경변수 기반)
    ├── application-local.yml             # 로컬 개발 설정
    ├── templates/
    │   ├── common.html                   # SPA 공통 레이아웃
    │   ├── layout/header.html            # 헤더 + SSE 초기화
    │   ├── layout/nav.html               # 하단 네비게이션
    │   ├── auth/                         # 로그인, 회원가입, 계정찾기
    │   ├── finance/finance.html
    │   ├── usedmarket/usedmarket.html
    │   ├── community/community.html
    │   ├── timesale/                     # 타임세일 + 상세
    │   ├── chat/                         # AI 채팅, 채팅방
    │   ├── notification/
    │   └── profile/
    └── static/
        ├── css/common.css                # 공통 + 모듈별 CSS
        └── js/                           # sse-client.js, 각 모듈 JS
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

## 환경 변수

| 변수명 | 설명 |
|--------|------|
| `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD` | MySQL 접속 정보 |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis 접속 정보 |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail SMTP |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) |
| `GOOGLE_MAPS_API_KEY` | Google Maps API |
| `UPLOAD_PATH` | 파일 업로드 경로 (기본: `/upload`) |
| `SPRING_PROFILES_ACTIVE` | 프로필 (`local` / `prod`) |

로컬 개발 시 `application-local.yml`에 Groq API 키 포함.

---

## 빌드 및 실행

```bash
# 빌드 (Windows)
gradlew.bat clean build -x test

# 실행
gradlew.bat bootRun

# 테스트 (H2 인메모리 DB 자동 사용)
gradlew.bat test
```

접속: `http://localhost:8080`

---

## 주요 설계 결정

- **API 응답 표준**: 모든 REST API는 `ApiResponse<T>` 래퍼 반환
- **예외 처리**: `CustomException(ErrorCode)` → `GlobalExceptionHandler` 중앙 처리
- **JPA 네이밍**: `PhysicalNamingStrategyStandardImpl` — `@Column(name=...)` 명시 필요
- **트랜잭션 후 비동기**: `TransactionSynchronizationManager`로 커밋 후 알림 발송
- **조회수 중복 방지**: Redis Set (`USED:{id}:viewers`) + 24시간 TTL
- **AI Tool**: 요청마다 서비스 주입 + userIdx를 갖는 POJO 인스턴스 생성 (Thread-safe)
- **SSE + Virtual Thread**: Java 21 Virtual Thread Executor로 비동기 스트리밍 지원
