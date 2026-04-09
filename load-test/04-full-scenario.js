/**
 * 04-full-scenario.js - 복합 사용자 시나리오 (포트폴리오 핵심 테스트)
 *
 * 목적: 실제 서비스와 유사한 혼합 트래픽 패턴으로 전체적인 성능 측정
 *       - 읽기 전용 유저 70% (커뮤니티 탐색)
 *       - 타임세일 조회 유저 20% (상점/상품 목록)
 *       - 중간마다 서버 상태 스냅샷 수집
 *
 * 실행:
 *   k6 run 04-full-scenario.js \
 *     -e BASE_URL=https://your-domain.com \
 *     -e TEST_EMAIL=test@example.com \
 *     -e TEST_PASSWORD=yourpassword \
 *     --out json=results/full-scenario.json   (결과 저장)
 */

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL   = __ENV.BASE_URL      || 'https://your-domain.com';
const TEST_EMAIL = __ENV.TEST_EMAIL    || 'test@example.com';
const TEST_PASS  = __ENV.TEST_PASSWORD || 'password';

// 시나리오별 메트릭
const communityLatency  = new Trend('community_latency');
const timesaleLatency   = new Trend('timesale_latency');
const overallErrorRate  = new Rate('overall_error_rate');
const totalRequests     = new Counter('total_requests');

// 수집된 서버 스냅샷 (테스트 종료 후 리포트용)
const snapshots = [];

export const options = {
  scenarios: {
    // 시나리오 A: 커뮤니티 탐색 (메인 트래픽)
    community_browsing: {
      executor:       'ramping-vus',
      startVUs:       0,
      stages: [
        { duration: '1m',  target: 15 },
        { duration: '3m',  target: 25 },  // 안정 부하
        { duration: '1m',  target: 40 },  // 피크
        { duration: '1m',  target: 0  },
      ],
      gracefulRampDown: '30s',
      exec: 'communityScenario',
    },
    // 시나리오 B: 타임세일 조회 (보조 트래픽)
    timesale_browsing: {
      executor:   'constant-vus',
      vus:        8,
      duration:   '6m',
      startTime:  '30s',  // 커뮤니티 워밍업 후 시작
      exec:       'timesaleScenario',
    },
    // 시나리오 C: 서버 상태 모니터링 (별도 VU)
    monitor: {
      executor:  'constant-vus',
      vus:       1,
      duration:  '6m',
      exec:      'monitorScenario',
    },
  },
  thresholds: {
    http_req_duration:   ['p(50)<1000', 'p(95)<3000', 'p(99)<5000'],
    http_req_failed:     ['rate<0.05'],
    community_latency:   ['p(95)<3000'],
    timesale_latency:    ['p(95)<2000'],
    overall_error_rate:  ['rate<0.05'],
  },
};

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/auth/login-proc`,
    { userEmail: TEST_EMAIL, password: TEST_PASS },
    { redirects: 0 }
  );

  const sessionCookie = (loginRes.cookies['SESSION'] || [])[0]?.value;
  if (!sessionCookie) {
    fail(`로그인 실패! TEST_EMAIL/TEST_PASSWORD를 확인하세요. status=${loginRes.status}`);
  }

  console.log('=== 부하 테스트 시작 ===');
  console.log(`대상 서버: ${BASE_URL}`);
  console.log(`커뮤니티 탐색 유저: 최대 40명`);
  console.log(`타임세일 조회 유저: 8명`);

  return { sessionCookie };
}

// 시나리오 A: 커뮤니티 탐색
export function communityScenario({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  const page = randomIntBetween(1, 5);

  // 커뮤니티 목록 조회
  const listRes = http.get(`${BASE_URL}/community/list?page=${page}`, {
    headers: { 'Accept': 'application/json' },
    tags: { name: 'community_list' },
  });
  communityLatency.add(listRes.timings.duration);
  totalRequests.add(1);

  const listOk = check(listRes, {
    'community list 200':    (r) => r.status === 200,
    'community list valid':  (r) => { try { return Array.isArray(JSON.parse(r.body)); } catch(e) { return false; } },
  });
  overallErrorRate.add(!listOk);

  // 목록에서 랜덤 게시글 상세 조회
  if (listOk && listRes.status === 200) {
    const boards = JSON.parse(listRes.body);
    if (boards.length > 0) {
      const board = boards[randomIntBetween(0, boards.length - 1)];

      sleep(randomIntBetween(1, 3));  // 게시글 읽는 시간 시뮬레이션

      const detailRes = http.get(`${BASE_URL}/community/view/${board.boardIdx}`, {
        tags: { name: 'community_detail' },
      });
      communityLatency.add(detailRes.timings.duration);
      totalRequests.add(1);

      const detailOk = check(detailRes, {
        'community detail 200': (r) => r.status === 200,
      });
      overallErrorRate.add(!detailOk);
    }
  }

  sleep(randomIntBetween(2, 5));
}

// 시나리오 B: 타임세일 조회
export function timesaleScenario({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  // 가게 목록 조회
  const storeRes = http.get(`${BASE_URL}/timesale/store/list`, {
    tags: { name: 'timesale_stores' },
  });
  timesaleLatency.add(storeRes.timings.duration);
  totalRequests.add(1);

  check(storeRes, { 'store list 200': (r) => r.status === 200 });

  if (storeRes.status === 200) {
    const stores = JSON.parse(storeRes.body);

    if (stores.length > 0) {
      const store = stores[randomIntBetween(0, Math.min(stores.length - 1, 4))];
      sleep(1);

      // 가게 상세 + 상품 목록 병렬 조회
      const responses = http.batch([
        ['GET', `${BASE_URL}/timesale/detail/${store.storeIdx}`,         null, { tags: { name: 'store_detail' } }],
        ['GET', `${BASE_URL}/timesale/detail/${store.storeIdx}/products`, null, { tags: { name: 'store_products' } }],
      ]);

      responses.forEach((r) => {
        timesaleLatency.add(r.timings.duration);
        totalRequests.add(1);
        overallErrorRate.add(r.status !== 200);
      });
    }
  }

  sleep(randomIntBetween(3, 6));
}

// 시나리오 C: 서버 모니터링 (30초마다 스냅샷)
export function monitorScenario() {
  const res = http.get(`${BASE_URL}/api/internal/stats`);

  if (res.status === 200) {
    const s = JSON.parse(res.body);
    const snapshot = {
      time:            new Date().toISOString(),
      cpuUsage:        s.cpuUsage,
      memoryUsage:     s.memoryUsage,
      hikari_active:   s.hikari_active,
      hikari_pending:  s.hikari_pending,
      hikari_total:    s.hikari_total,
      activeUsers:     s.activeUsers,
    };

    console.log(
      `[MONITOR] CPU: ${s.cpuUsage}% | MEM: ${s.memoryUsage}% | ` +
      `HikariCP: ${s.hikari_active}/${s.hikari_total} active, ${s.hikari_pending} pending`
    );
  }

  sleep(30);  // 30초마다 측정
}

export function teardown() {
  console.log('=== 부하 테스트 완료 ===');
  console.log('k6 summary 결과를 확인하세요.');
  console.log('--out json=results/full-scenario.json 옵션으로 상세 결과를 저장할 수 있습니다.');
}
