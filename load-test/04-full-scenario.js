/**
 * 04-full-scenario.js - 복합 사용자 시나리오 (포트폴리오 핵심 테스트)
 * 실행: k6 run 04-full-scenario.js -e BASE_URL=http://soragodong.duckdns.org -e TEST_EMAIL=xxx -e TEST_PASSWORD=xxx
 *
 * StoreDto 필드:        storeIdx, storeName, storeAddress, ...
 * StoreProductDto 필드: productNum, storeIdx, productName, price, eventPrice, productQuantity, ...
 * BoardDto 필드:        boardIdx, boardTitle, boardContent, ...
 */

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL   = __ENV.BASE_URL      || 'http://soragodong.duckdns.org';
const TEST_EMAIL = __ENV.TEST_EMAIL    || 'test@example.com';
const TEST_PASS  = __ENV.TEST_PASSWORD || 'password';

const communityLatency = new Trend('community_latency');
const timesaleLatency  = new Trend('timesale_latency');
const errorRate        = new Rate('overall_error_rate');
const totalRequests    = new Counter('total_requests');

export const options = {
  scenarios: {
    community_browsing: {
      executor:    'ramping-vus',
      startVUs:    0,
      stages: [
        { duration: '1m',  target: 15 },
        { duration: '3m',  target: 25 },
        { duration: '1m',  target: 40 },
        { duration: '1m',  target: 0  },
      ],
      gracefulRampDown: '30s',
      exec: 'communityScenario',
    },
    timesale_browsing: {
      executor:  'constant-vus',
      vus:       8,
      duration:  '6m',
      startTime: '30s',
      exec:      'timesaleScenario',
    },
    monitor: {
      executor: 'constant-vus',
      vus:      1,
      duration: '6m',
      exec:     'monitorScenario',
    },
  },
  thresholds: {
    http_req_duration:  ['p(50)<1000', 'p(95)<3000', 'p(99)<5000'],
    http_req_failed:    ['rate<0.05'],
    community_latency:  ['p(95)<3000'],
    timesale_latency:   ['p(95)<2000'],
    overall_error_rate: ['rate<0.05'],
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
    fail(`로그인 실패 - status: ${loginRes.status}`);
  }

  console.log('=== 복합 부하 테스트 시작 ===');
  return { sessionCookie };
}

// 시나리오 A: 커뮤니티 탐색 (BoardDto 기반)
export function communityScenario({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  const page = randomIntBetween(1, 2);
  const listRes = http.get(`${BASE_URL}/community/list?page=${page}`, {
    headers: { 'Accept': 'application/json' },
    tags: { name: 'community_list' },
  });
  communityLatency.add(listRes.timings.duration);
  totalRequests.add(1);

  let boards = [];
  try { boards = JSON.parse(listRes.body); } catch (e) { errorRate.add(true); sleep(2); return; }

  const listOk = check(listRes, {
    'community list 200':   (r) => r.status === 200,
    'community list valid': () => Array.isArray(boards),
  });
  errorRate.add(!listOk);

  if (listOk && boards.length > 0) {
    // BoardDto.boardIdx 로 상세 조회
    const boardIdx = boards[randomIntBetween(0, boards.length - 1)].boardIdx;
    sleep(randomIntBetween(1, 3));

    const detailRes = http.get(`${BASE_URL}/community/view/${boardIdx}`, {
      headers: { 'Accept': 'application/json' },
      tags: { name: 'community_detail' },
    });
    communityLatency.add(detailRes.timings.duration);
    totalRequests.add(1);

    let detail = null;
    try { detail = JSON.parse(detailRes.body); } catch (e) { errorRate.add(true); }

    errorRate.add(!check(detailRes, {
      'community detail 200':      (r) => r.status === 200,
      'community detail boardIdx': () => detail?.boardIdx !== undefined,
    }));
  }

  sleep(randomIntBetween(2, 5));
}

// 시나리오 B: 타임세일 조회 (StoreDto, StoreProductDto 기반)
export function timesaleScenario({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  // 가게 목록 조회 - StoreDto[]
  const storeRes = http.get(`${BASE_URL}/timesale/store/list`, {
    headers: { 'Accept': 'application/json' },
    tags: { name: 'timesale_stores' },
  });
  timesaleLatency.add(storeRes.timings.duration);
  totalRequests.add(1);
  errorRate.add(storeRes.status !== 200);

  if (storeRes.status === 200) {
    let stores = [];
    try { stores = JSON.parse(storeRes.body); } catch (e) { errorRate.add(true); sleep(3); return; }

    if (stores.length > 0) {
      // StoreDto.storeIdx 사용
      const storeIdx = stores[randomIntBetween(0, Math.min(stores.length - 1, 4))].storeIdx;
      sleep(1);

      // 가게 상세 + 상품 목록 병렬 조회
      const responses = http.batch([
        ['GET', `${BASE_URL}/timesale/detail/${storeIdx}`,          null, { headers: { 'Accept': 'application/json' }, tags: { name: 'store_detail' } }],
        ['GET', `${BASE_URL}/timesale/detail/${storeIdx}/products`, null, { headers: { 'Accept': 'application/json' }, tags: { name: 'store_products' } }],
      ]);

      responses.forEach((r) => {
        timesaleLatency.add(r.timings.duration);
        totalRequests.add(1);
        errorRate.add(r.status !== 200);
      });
    }
  }

  sleep(randomIntBetween(3, 6));
}

// 시나리오 C: 서버 상태 모니터링 (30초마다)
export function monitorScenario() {
  const res = http.get(`${BASE_URL}/api/internal/stats`, {
    headers: { 'Accept': 'application/json', 'X-Monitor-Token': 'sora-monitor-2025' },
  });

  if (res.status === 200) {
    const s = JSON.parse(res.body);
    console.log(
      `[MONITOR] CPU: ${s.cpuUsage}% | MEM: ${s.memoryUsage}% | ` +
      `HikariCP: ${s.hikari_active}/${s.hikari_total} active, ${s.hikari_pending} pending`
    );
  }

  sleep(30);
}

export function teardown() {
  console.log('=== 복합 부하 테스트 완료 ===');
}
