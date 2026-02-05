# 🐚 소라고동 (Sora Godong)

> AI 기반 금융 커뮤니티 플랫폼

## 📋 프로젝트 개요

**소라고동**은 사용자들이 금융 정보를 공유하고, 실시간 채팅으로 소통하며, AI 어시스턴트의 도움을 받을 수 있는 통합 금융 커뮤니티 플랫폼입니다.

### 주요 기능
- 👤 **회원 관리**: 안전한 회원가입/로그인 (BCrypt 암호화)
- 📧 **이메일 시스템**: 환영 이메일, 계정 인증, 비밀번호 재설정
- 💬 **실시간 채팅**: WebSocket 기반 실시간 메시징
- 🤖 **AI 어시스턴트**: OpenAI 기반 금융 상담
- 💰 **금융 기능**: 계좌 관리, 시장 분석, 커뮤니티
- 🎨 **현대적 UI/UX**: 반응형 디자인, SPA 네비게이션

---

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.11
- **보안**: Spring Security, BCryptPasswordEncoder
- **데이터베이스**: MySQL 8.0
- **캐싱/세션**: Redis
- **메시징**: WebSocket
- **이메일**: JavaMailSender (Gmail SMTP)
- **ORM**: Spring Data JPA
- **템플릿**: Thymeleaf

### Frontend
- **Markup**: HTML5, Thymeleaf
- **Styling**: CSS3
- **Scripting**: Vanilla JavaScript (Promise-based)
- **라이브러리**: Chart.js, Ionicons

### DevOps
- **빌드**: Gradle
- **컨테이너**: Docker & Docker Compose
- **JVM**: Java 21

---

## 📦 설치 및 실행

### 사전 요구사항
- **Java**: JDK 21 이상
- **MySQL**: 8.0 이상
- **Redis**: 최신 버전
- **Gradle**: 8.0 이상 (gradlew 포함)

### 1️⃣ 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하거나, IDE의 Run Configuration에서 환경변수를 설정합니다:

```bash
# 데이터베이스
export DB_HOST=<your-mysql-host>
export DB_PORT=<your-mysql-port>
export DB_USERNAME=<your-db-username>
export DB_PASSWORD=<your-db-password>

# Redis
export REDIS_HOST=<your-redis-host>
export REDIS_PORT=<your-redis-port>
export REDIS_PASSWORD=<your-redis-password>

# 이메일
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=<your-email@gmail.com>
export MAIL_PASSWORD=<your-gmail-app-password>

# JWT & Security
export JWT_SECRET=<your-32-character-secret-key>

# API Keys
export OPENAI_API_KEY=<your-openai-api-key>
export GOOGLE_MAPS_API_KEY=<your-google-maps-api-key>

# 프로필
export SPRING_PROFILES_ACTIVE=local
```

> **💡 Tip**: 로컬 개발 환경에서는 `application-local.yml`이 자동으로 기본값을 제공합니다.

### 2️⃣ 데이터베이스 초기화

```bash
# MySQL 접속
mysql -h <your-mysql-host> -u <your-db-username> -p

# 데이터베이스 생성
CREATE DATABASE soragodong;
USE soragodong;
```

### 3️⃣ 빌드

```bash
# Gradle 빌드 (Linux/Mac)
./gradlew clean build

# Gradle 빌드 (Windows)
gradlew.bat clean build
```

### 4️⃣ 실행

```bash
# IDE에서 실행 (권장)
# Run → SoragodongApplication.java

# 또는 명령어로 실행
./gradlew bootRun

# JAR 파일로 실행
java -jar build/libs/sora-api-0.0.1-SNAPSHOT.jar
```

### 5️⃣ 접속

```
🌐 http://localhost:8080
```

---

## 📁 프로젝트 구조

```
sora-api/
├── src/
│   ├── main/
│   │   ├── java/com/scit/soragodong/
│   │   │   ├── SoragodongApplication.java      # 메인 진입점
│   │   │   ├── controller/                      # REST 컨트롤러
│   │   │   │   ├── AuthController.java          # 인증 관련
│   │   │   │   └── HomeController.java          # 홈 페이지
│   │   │   ├── service/                         # 비즈니스 로직
│   │   │   │   ├── UserService.java
│   │   │   │   ├── EmailService.java            # 이메일 전송
│   │   │   │   └── SseService.java              # 실시간 알림
│   │   │   ├── repository/                      # 데이터 접근
│   │   │   │   └── UserRepository.java
│   │   │   ├── domain/                          # 도메인 모델
│   │   │   │   ├── entity/                      # JPA 엔티티
│   │   │   │   ├── dto/                         # Data Transfer Objects
│   │   │   │   └── enums/                       # 열거형
│   │   │   ├── config/                          # 설정 클래스
│   │   │   │   ├── SecurityConfig.java          # Spring Security
│   │   │   │   ├── RedisConfig.java             # Redis 설정
│   │   │   │   └── WebSocketConfig.java         # WebSocket 설정
│   │   │   ├── exception/                       # 예외 처리
│   │   │   │   ├── CustomException.java
│   │   │   │   ├── ErrorCode.java               # 에러 코드 enum
│   │   │   │   └── GlobalExceptionHandler.java  # 전역 예외 핸들러
│   │   │   └── util/                            # 유틸리티
│   │   └── resources/
│   │       ├── application.yml                  # 프로덕션 설정
│   │       ├── application-local.yml            # 로컬 설정
│   │       ├── application-prod.yml             # 프로덕션 프로필
│   │       ├── templates/                       # Thymeleaf 템플릿
│   │       │   ├── common.html                  # 공통 레이아웃
│   │       │   ├── index.html                   # 메인 페이지
│   │       │   ├── auth/                        # 인증 페이지
│   │       │   │   ├── login.html
│   │       │   │   ├── signup.html
│   │       │   │   └── findAccount.html
│   │       │   └── layout/                      # 레이아웃 조각
│   │       │       ├── header.html
│   │       │       └── nav.html
│   │       └── static/
│   │           ├── css/                         # 스타일시트
│   │           │   ├── common.css
│   │           │   ├── finance.css
│   │           │   └── ...
│   │           └── js/                          # 클라이언트 스크립트
│   │               ├── api.js                   # API 요청 래퍼
│   │               ├── utils.js                 # 유틸리티
│   │               └── validation.js            # 폼 검증
│   └── test/
│       └── java/                                # 테스트 코드
├── build.gradle                                 # Gradle 설정
├── settings.gradle
├── compose.yaml                                 # Docker Compose
└── README.md                                    # 이 파일
```

---

## 🔐 인증 및 보안

### 회원가입 프로세스
1. 사용자가 이메일과 비밀번호로 가입
2. 비밀번호는 **BCrypt**로 암호화되어 저장
3. 환영 이메일 비동기 발송 (`@Async`)
4. 세션 정보는 **Redis**에 저장 (30분 TTL)

### 로그인 프로세스
1. Spring Security의 `AuthenticationManager` 사용
2. `CustomUserDetailsService`에서 사용자 로드
3. BCrypt 비밀번호 검증
4. 성공 시 JSON 응답 + 세션 생성
5. 실패 시 `ErrorCode.LOGIN_FAILED` 에러

### 에러 처리
- **중앙화된 에러 관리**: `GlobalExceptionHandler`
- **표준 에러 코드**: `ErrorCode` enum
- **일관된 응답 포맷**: `ApiResponse<T>`

---

## 📧 이메일 시스템

### 지원하는 이메일 템플릿

#### 1. 환영 이메일 (Welcome)
- 회원가입 완료 후 자동 발송
- HTML 기반 디자인
- 비동기 처리 (`@Async`)

#### 2. 인증 이메일 (Authentication)
- 특정 작업 검증 시 발송
- OTP 또는 인증 링크 포함

#### 3. 비밀번호 재설정 (Password Reset)
- 비밀번호 찾기 요청 시 발송
- 재설정 링크 포함

### 이메일 발송 설정
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # Gmail 앱 비밀번호
```

---

## 🗄 데이터베이스 스키마

### User 테이블
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 🚀 배포 (Production)

### 환경변수 설정 (서버에서)
```bash
# 프로덕션 환경변수
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=<your-prod-db-host>
export DB_PORT=<your-db-port>
export DB_USERNAME=<your-prod-username>
export DB_PASSWORD=<your-prod-password>
export REDIS_HOST=<your-prod-redis-host>
# ... 기타 필수 환경변수
```

### Docker 배포
```bash
# 이미지 빌드
docker build -t soragodong:latest .

# 컨테이너 실행
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  # ... 기타 환경변수
  soragodong:latest
```

### Docker Compose (개발용)
```bash
docker-compose up -d
```

---

## 📝 API 문서

### 인증 관련

#### 회원가입
```http
POST /auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동",
  "phone": "010-1234-5678"
}

Response: 200 OK
{
  "success": true,
  "message": "회원가입 완료"
}
```

#### 로그인
```http
POST /auth/login-proc
Content-Type: application/x-www-form-urlencoded

email=user@example.com&password=password123

Response: 200 OK
{
  "success": true,
  "message": "로그인 완료"
}
Set-Cookie: JSESSIONID=...
```

#### 이메일 중복 확인
```http
POST /auth/check-email
Content-Type: application/json

{
  "email": "user@example.com"
}

Response: 200 OK
{
  "available": true
}
```

---

## 🐛 문제 해결

### Redis 연결 실패
```
Error: Cannot get a resource, pool error Timeout waiting for idle object
```
**해결책**: Redis 서버 상태 확인
```bash
redis-cli -h <your-redis-host> -p <your-redis-port> ping
# PONG 응답이 오면 정상
```

### MySQL 연결 실패
```
Error: Communications link failure
```
**해결책**: MySQL 서버 실행 확인 및 연결 정보 검증
```bash
mysql -h <your-mysql-host> -u <your-username> -p
```

### 이메일 발송 실패
```
Error: Authentication failed; nested exception is javax.mail.AuthenticationFailedException
```
**해결책**: Gmail 앱 비밀번호 확인
- [Google 계정 보안](https://myaccount.google.com/security)에서 앱 비밀번호 재생성

---

## 📚 참고 자료

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Session](https://spring.io/projects/spring-session)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Redis](https://redis.io/docs/)

---

## 👥 팀 정보

- **프로젝트 이름**: 소라고동 (Sora Godong)
- **버전**: 1.0.0
- **라이선스**: MIT

---

## 📞 지원

문제가 발생하면 [이슈](../../issues)를 생성해주세요.

---

**Happy Coding! 🚀**
