<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:4f46e5,100:06b6d4&height=180&section=header&text=sora-api&fontSize=55&fontColor=ffffff&fontAlignY=38&desc=AI%20Lifestyle%20Platform&descAlignY=58&descSize=22" width="100%"/>
</div>

<div align="center">

[![Live](https://img.shields.io/badge/🌐_Live-soragodong.duckdns.org-06b6d4?style=for-the-badge)](http://soragodong.duckdns.org/)
&nbsp;
[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)]()
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)]()
[![Spring AI](https://img.shields.io/badge/Spring_AI_1.1.2-6DB33F?style=flat-square&logo=spring&logoColor=white)]()
[![Redis](https://img.shields.io/badge/Redis-FF4438?style=flat-square&logo=redis&logoColor=white)]()
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)]()

</div>

---

## 소개

AI 어시스턴트 **"소라 고동"** 이 자연어 명령으로 중고마켓·타임세일·가계부·커뮤니티를 한 번에 처리하는 통합 생활 플랫폼입니다.
Function Calling 기반으로 사용자의 말 한마디가 실제 서비스 로직으로 연결됩니다.

---

## 주요 기능

| 모듈 | 기능 |
|------|------|
| 🤖 **AI 어시스턴트** | Groq API (llama-3.3-70b) + Spring AI Function Calling으로 자연어 명령 실행 |
| 💰 **가계부** | 수입·지출 등록, 월별 예산 설정, Chart.js 시각화 |
| 🛒 **중고마켓** | 상품 등록·찜·구매, 키워드 알림, 위치 기반 거래 |
| 💬 **커뮤니티** | 게시글·댓글·좋아요, 무한스크롤, 검색 |
| ⚡ **타임세일** | Google Maps 매장 연동, Redis 원자적 재고 동시성 제어 |
| 🔔 **실시간 알림** | SSE 기반 키워드·상태변경·댓글 알림 |
| 💬 **실시간 채팅** | WebSocket (STOMP/SockJS) 1:1 메시징 |
| 🔐 **인증** | 이메일 인증, BCrypt 암호화, Redis Session |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Backend** | Java 21, Spring Boot 3.5, Spring Security, Spring AI 1.1.2 |
| **AI** | Groq API (llama-3.3-70b-versatile), Function Calling |
| **Database** | MySQL 8.0 (prod), H2 (test) |
| **Cache / Session** | Redis + Spring Session |
| **Real-time** | SSE, WebSocket (STOMP/SockJS) |
| **Frontend** | Thymeleaf, Vanilla JS, CSS3, Chart.js |
| **Infra** | Docker Compose, Nginx, Oracle Cloud, GitHub Actions CI/CD |

---

## 성능 개선

k6 부하 테스트 기반으로 발견한 프로덕션 이슈를 해결했습니다.

| 지표 | 개선 전 | 개선 후 |
|------|---------|---------|
| **에러율** | 99.3% | **0%** |
| **p(95) 응답시간** | ~1,400ms | **501ms (63%↓)** |
| **동시접속** | — | **49 CCU** 안정 처리 |

- 컨트롤러 레벨 중복 트랜잭션 제거 → 에러율 0% 달성
- JOIN FETCH 쿼리 최적화 → 응답시간 63% 단축
- Redis 원자적 연산으로 타임세일 동시성 제어

---

## 아키텍처

```
Client (Browser)
    |
    +-- HTTP/WebSocket --> Nginx (Reverse Proxy)
                               |
                         Spring Boot App
                         +-- Spring Security (Session/BCrypt)
                         +-- Spring AI --> Groq API (Function Calling)
                         +-- SSE (실시간 알림)
                         +-- WebSocket/STOMP (채팅)
                         +-- MySQL 8.0 (메인 DB)
                         +-- Redis (세션 / 캐시 / 동시성)
                               |
    GitHub Actions CI/CD --> Docker Compose --> Oracle Cloud
```

---

## 로컬 실행

### 필수 요구사항
- Java 21+
- Docker & Docker Compose
- Groq API Key

### 실행

```bash
git clone https://github.com/M1NiDRAG0N/sora-api.git
cd sora-api
```

`.env` 파일 생성:

```env
GROQ_API_KEY=your_groq_api_key
MYSQL_PASSWORD=your_db_password
REDIS_PASSWORD=your_redis_password
```

```bash
docker-compose up -d
```

> `http://localhost:8080` 접속

---

## 프로젝트 구조

```
src/main/java/
+-- ai/            # Spring AI Function Calling 도구 정의
+-- domain/
|   +-- ledger/    # 가계부
|   +-- market/    # 중고마켓
|   +-- community/ # 커뮤니티
|   +-- timesale/  # 타임세일
|   +-- chat/      # 실시간 채팅
|   +-- notify/    # SSE 알림
+-- config/        # Security, Redis, WebSocket 설정
+-- global/        # 공통 예외처리, 응답 포맷
```

---

<div align="center">

[![GitHub](https://img.shields.io/badge/GitHub-M1NiDRAG0N-181717?style=for-the-badge&logo=github)](https://github.com/M1NiDRAG0N)
[![Portfolio](https://img.shields.io/badge/Portfolio-m1nidrag0n.github.io-06b6d4?style=for-the-badge&logo=vercel&logoColor=white)](https://m1nidrag0n.github.io/portfolio/)

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:06b6d4,100:4f46e5&height=100&section=footer" width="100%"/>

</div>
