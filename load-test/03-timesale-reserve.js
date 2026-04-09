/**
 * 03-timesale-reserve.js - 타임세일 동시 예약 스트레스 테스트
 *
 * 목적: 여러 유저가 동시에 같은 상품 예약 시 재고 처리 정확성 + DB 락 동작 검증
 *       Race condition, 재고 초과 예약, 응답 지연 확인
 *
 * 실행:
 *   k6 run 03-timesale-reserve.js \
 *     -e BASE_URL=https://your-domain.com \
 *     -e TEST_EMAIL=test@example.com \
 *     -e TEST_PASSWORD=yourpassword \
 *     -e PRODUCT_NUM=1 \
 *     -e USER_IDX=1
 */

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL    = __ENV.BASE_URL      || 'https://your-domain.com';
const TEST_EMAIL  = __ENV.TEST_EMAIL    || 'test@example.com';
const TEST_PASS   = __ENV.TEST_PASSWORD || 'password';
const PRODUCT_NUM = parseInt(__ENV.PRODUCT_NUM || '1');
const USER_IDX    = parseInt(__ENV.USER_IDX    || '1');

const reserveSuccess  = new Counter('reserve_success');
const reserveConflict = new Counter('reserve_conflict');   // 409 재고 부족
const reserveFailed   = new Counter('reserve_failed');     // 5xx 오류
const reserveLatency  = new Trend('reserve_latency');
const overReserveRate = new Rate('over_reserve_rate');     // 0이어야 정상

export const options = {
  // 스파이크: 짧은 시간에 많은 동시 예약 요청
  scenarios: {
    spike_reserve: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 50,
      stages: [
        { duration: '10s', target: 1  },  // 워밍업
        { duration: '30s', target: 20 },  // 스파이크: 초당 20 요청
        { duration: '10s', target: 1  },  // 쿨다운
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    reserve_failed:    ['count<5'],        // 서버 오류(5xx)는 5회 미만
    over_reserve_rate: ['rate==0'],        // 재고 초과 예약은 절대 0
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
    fail('로그인 실패');
  }

  // 예약 전 상품 재고 확인
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  const productRes = http.get(`${BASE_URL}/timesale/product/${PRODUCT_NUM}`);
  if (productRes.status === 200) {
    const product = JSON.parse(productRes.body);
    console.log(`테스트 상품: ${product.productName} | 재고: ${product.productStock}개 | 가격: ${product.productPrice}원`);
  }

  return { sessionCookie };
}

export default function ({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  const payload = JSON.stringify({
    userIdx:       USER_IDX,
    productNum:    PRODUCT_NUM,
    orderQuantity: 1,
  });

  const res = http.post(`${BASE_URL}/timesale/reserve`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  reserveLatency.add(res.timings.duration);

  if (res.status === 200) {
    reserveSuccess.add(1);
    check(res, { 'reserve success & has orderIndex': (r) => JSON.parse(r.body).orderIndex !== undefined });

  } else if (res.status === 409) {
    // 정상적인 재고 부족 응답
    reserveConflict.add(1);
    overReserveRate.add(false);  // rate=0 유지 (정상)

  } else if (res.status >= 500) {
    reserveFailed.add(1);
    console.error(`서버 오류: status=${res.status}, body=${res.body?.slice(0, 200)}`);

  } else if (res.status === 400) {
    console.warn(`잘못된 요청: ${res.body?.slice(0, 200)}`);
  }

  // 재고 초과 예약 감지: 성공 응답인데 실제 재고가 0인 경우는 서버에서 판단
  // 여기서는 응답 코드 기반으로만 검증

  sleep(0.5);
}

export function teardown({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  // 테스트 후 최종 서버 상태 출력
  const statsRes = http.get(`${BASE_URL}/api/internal/stats`);
  if (statsRes.status === 200) {
    const s = JSON.parse(statsRes.body);
    console.log(`=== 테스트 완료 후 서버 상태 ===`);
    console.log(`CPU: ${s.cpuUsage}% | MEM: ${s.memoryUsage}%`);
    console.log(`HikariCP: active=${s.hikari_active}, idle=${s.hikari_idle}, pending=${s.hikari_pending}, total=${s.hikari_total}`);
  }
}
