/**
 * 03-timesale-reserve.js - 타임세일 동시 예약 스파이크 테스트
 * 실행: k6 run 03-timesale-reserve.js \
 *        -e BASE_URL=http://soragodong.duckdns.org \
 *        -e TEST_EMAIL=xxx -e TEST_PASSWORD=xxx \
 *        -e PRODUCT_NUM=2 -e USER_IDX=1 -e STORE_IDX=1
 *
 * StoreProductDto 필드: productNum, storeIdx, storeName, category,
 *                       productName, price, eventPrice, productQuantity, productPictureIdx
 * UserOrderDto 필드:    orderIndex, userIdx, productNum, storeIdx,
 *                       productName, totalPrice, orderQuantity, orderTime,
 *                       orderStatus, cancelledAt, cancelReason
 */

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL    = __ENV.BASE_URL      || 'http://soragodong.duckdns.org';
const TEST_EMAIL  = __ENV.TEST_EMAIL    || 'test@example.com';
const TEST_PASS   = __ENV.TEST_PASSWORD || 'password';
const PRODUCT_NUM = parseInt(__ENV.PRODUCT_NUM || '1');
const USER_IDX    = parseInt(__ENV.USER_IDX    || '1');
const STORE_IDX   = parseInt(__ENV.STORE_IDX   || '1');

const reserveSuccess  = new Counter('reserve_success');
const reserveConflict = new Counter('reserve_conflict');
const reserveFailed   = new Counter('reserve_failed');
const reserveLatency  = new Trend('reserve_latency');

export const options = {
  scenarios: {
    spike_reserve: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 50,
      stages: [
        { duration: '10s', target: 1  },
        { duration: '30s', target: 20 },
        { duration: '10s', target: 1  },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    reserve_failed:    ['count<5'],
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

  // 상품 정보 조회 - StoreProductDto
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  const productRes = http.get(`${BASE_URL}/timesale/product/${PRODUCT_NUM}`, {
    headers: { 'Accept': 'application/json' },
  });

  if (productRes.status !== 200) {
    fail(`상품 조회 실패 - status: ${productRes.status}`);
  }

  const product = JSON.parse(productRes.body);
  // StoreProductDto: productName, eventPrice(타임세일 가격), productQuantity(재고)
  console.log(`상품: ${product.productName} | 재고: ${product.productQuantity}개 | 타임세일가: ${product.eventPrice}원`);

  return {
    sessionCookie,
    productName: product.productName,
    totalPrice:  product.eventPrice,  // 타임세일 가격
  };
}

export default function ({ sessionCookie, productName, totalPrice }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  // UserOrderDto에 필요한 모든 필드 포함
  const payload = JSON.stringify({
    userIdx:       USER_IDX,
    productNum:    PRODUCT_NUM,
    storeIdx:      STORE_IDX,
    productName:   productName,
    totalPrice:    totalPrice,
    orderQuantity: 1,
  });

  const res = http.post(`${BASE_URL}/timesale/reserve`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Accept':       'application/json',
    },
  });

  reserveLatency.add(res.timings.duration);

  if (res.status === 200) {
    reserveSuccess.add(1);
    check(res, {
      'reserve has orderIndex': () => JSON.parse(res.body)?.orderIndex !== undefined,
    });
  } else if (res.status === 409) {
    reserveConflict.add(1);
    console.log(`재고 부족 (정상): ${res.body?.slice(0, 80)}`);
  } else if (res.status >= 500) {
    reserveFailed.add(1);
    console.error(`서버 오류 - status: ${res.status} | body: ${res.body?.slice(0, 150)}`);
  } else {
    console.warn(`예상 외 응답 - status: ${res.status} | body: ${res.body?.slice(0, 150)}`);
  }

  sleep(0.5);
}

export function teardown({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  const statsRes = http.get(`${BASE_URL}/api/internal/stats`, {
    headers: { 'Accept': 'application/json' },
  });
  if (statsRes.status === 200) {
    const s = JSON.parse(statsRes.body);
    console.log(`=== 테스트 완료 후 서버 상태 ===`);
    console.log(`CPU: ${s.cpuUsage}% | MEM: ${s.memoryUsage}%`);
    console.log(`HikariCP: ${s.hikari_active}/${s.hikari_total} active, ${s.hikari_pending} pending`);
  }
}
