/**
 * 02-community.js - 커뮤니티 부하 테스트 (인증 필요)
 *
 * 목적: 실제 로그인 → 커뮤니티 목록 조회 → 상세 조회 흐름 부하 테스트
 *       HikariCP 풀 한계(max=20) 도달 여부 관찰
 *
 * 실행:
 *   k6 run 02-community.js \
 *     -e BASE_URL=https://your-domain.com \
 *     -e TEST_EMAIL=test@example.com \
 *     -e TEST_PASSWORD=yourpassword
 */

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

const BASE_URL   = __ENV.BASE_URL      || 'https://your-domain.com';
const TEST_EMAIL = __ENV.TEST_EMAIL    || 'test@example.com';
const TEST_PASS  = __ENV.TEST_PASSWORD || 'password';

// 커스텀 메트릭
const listLatency   = new Trend('community_list_latency');
const detailLatency = new Trend('community_detail_latency');
const errorRate     = new Rate('error_rate');

export const options = {
  scenarios: {
    // 시나리오 1: 일반 탐색 유저 (읽기 위주)
    browsing_users: {
      executor:    'ramping-vus',
      startVUs:    0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '2m',  target: 30 },  // HikariCP max(20) 초과 시도
        { duration: '1m',  target: 30 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration:        ['p(95)<3000'],
    http_req_failed:          ['rate<0.1'],
    community_list_latency:   ['p(95)<2000'],
    community_detail_latency: ['p(95)<2000'],
    error_rate:               ['rate<0.1'],
  },
};

// setup(): 한 번만 실행 - 로그인해서 세션 쿠키 획득
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/auth/login-proc`,
    { userEmail: TEST_EMAIL, password: TEST_PASS },
    { redirects: 0 }  // 리다이렉트 따라가지 않고 쿠키만 추출
  );

  // Spring Security 폼 로그인은 302 리다이렉트 반환
  const sessionCookie = (loginRes.cookies['SESSION'] || [])[0]?.value;
  if (!sessionCookie) {
    console.error(`로그인 실패! status=${loginRes.status}, body=${loginRes.body?.slice(0, 200)}`);
    fail('로그인에 실패했습니다. TEST_EMAIL/TEST_PASSWORD 환경변수를 확인하세요.');
  }

  console.log(`로그인 성공 - SESSION 쿠키 획득`);
  return { sessionCookie };
}

export default function ({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  // 1. 커뮤니티 목록 (1페이지)
  const listRes = http.get(`${BASE_URL}/community/list?page=1`, {
    headers: { 'Accept': 'application/json' },
  });
  listLatency.add(listRes.timings.duration);

  const listOk = check(listRes, {
    'list status 200':       (r) => r.status === 200,
    'list is array':         (r) => Array.isArray(JSON.parse(r.body || '[]')),
    'list not empty':        (r) => (JSON.parse(r.body || '[]')).length > 0,
  });
  errorRate.add(!listOk);

  if (!listOk) {
    console.warn(`목록 조회 실패: status=${listRes.status}`);
    sleep(2);
    return;
  }

  // 2. 목록에서 첫 번째 게시글 상세 조회
  const boards = JSON.parse(listRes.body);
  if (boards.length > 0) {
    const boardIdx = boards[0].boardIdx;
    const detailRes = http.get(`${BASE_URL}/community/view/${boardIdx}`, {
      headers: { 'Accept': 'application/json' },
    });
    detailLatency.add(detailRes.timings.duration);

    const detailOk = check(detailRes, {
      'detail status 200': (r) => r.status === 200,
      'detail has content': (r) => JSON.parse(r.body || '{}').boardIdx !== undefined,
    });
    errorRate.add(!detailOk);
  }

  // 3. 서버 상태 사이드카 모니터링 (50번째 iter마다)
  if (__ITER % 50 === 0) {
    const statsRes = http.get(`${BASE_URL}/api/internal/stats`);
    if (statsRes.status === 200) {
      const s = JSON.parse(statsRes.body);
      console.log(`[iter ${__ITER}] CPU: ${s.cpuUsage}% | MEM: ${s.memoryUsage}% | ` +
                  `HikariActive: ${s.hikari_active}/${s.hikari_total} | Pending: ${s.hikari_pending}`);
    }
  }

  sleep(Math.random() * 2 + 1);  // 1~3초 랜덤 대기 (실사용자 행동 모방)
}
