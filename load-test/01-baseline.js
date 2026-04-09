/**
 * 01-baseline.js - 기준선 테스트 (인증 불필요)
 *
 * 목적: 인증 없이 접근 가능한 엔드포인트로 서버 기본 응답 성능 측정
 * 측정: /api/internal/stats (CPU, 메모리, HikariCP 커넥션 풀)
 *
 * 실행: k6 run 01-baseline.js -e BASE_URL=https://your-domain.com
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'https://your-domain.com';

// 커스텀 메트릭
const statsLatency = new Trend('stats_latency');
const requestsFailed = new Counter('requests_failed');

export const options = {
  stages: [
    { duration: '30s', target: 5 },   // 워밍업
    { duration: '1m',  target: 20 },  // 부하 증가
    { duration: '2m',  target: 20 },  // 유지
    { duration: '30s', target: 50 },  // 피크
    { duration: '30s', target: 0 },   // 쿨다운
  ],
  thresholds: {
    http_req_duration:     ['p(95)<2000'],  // 95%가 2초 이내
    http_req_failed:       ['rate<0.05'],   // 실패율 5% 이하
    stats_latency:         ['p(99)<3000'],
  },
};

export default function () {
  // 서버 리소스 상태 조회 (HikariCP 포함)
  const statsRes = http.get(`${BASE_URL}/api/internal/stats`, { redirects: 0 });
  statsLatency.add(statsRes.timings.duration);

  // 안전하게 JSON 파싱
  let body = null;
  try {
    body = JSON.parse(statsRes.body);
  } catch (e) {
    console.error(`JSON 파싱 실패 - status: ${statsRes.status} | location: ${statsRes.headers['Location']} | body: ${statsRes.body?.slice(0, 150)}`);
    requestsFailed.add(1);
    sleep(1);
    return;
  }

  const ok = check(statsRes, {
    'stats status 200':          (r) => r.status === 200,
    'stats has cpuUsage':        () => body.cpuUsage !== undefined,
    'stats has hikari_active':   () => body.hikari_active !== undefined,
    'stats hikari_pending < 5':  () => body.hikari_pending < 5,
  });

  if (!ok) requestsFailed.add(1);

  // 10번째 iter마다 출력 (원인 파악용으로 빈도 높임)
  if (__ITER % 10 === 0) {
    console.log(`[iter ${__ITER}] CPU: ${body.cpuUsage}% | MEM: ${body.memoryUsage}% | ` +
                `HikariCP: ${body.hikari_active}/${body.hikari_total} active, ${body.hikari_pending} pending`);
  }

  sleep(1);
}
