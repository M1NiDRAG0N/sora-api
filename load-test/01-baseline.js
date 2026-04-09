/**
 * 01-baseline.js - 기준선 테스트 (인증 불필요)
 * 실행: k6 run 01-baseline.js -e BASE_URL=http://soragodong.duckdns.org
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://soragodong.duckdns.org';

const statsLatency   = new Trend('stats_latency');
const requestsFailed = new Counter('requests_failed');

export const options = {
  stages: [
    { duration: '30s', target: 5  },
    { duration: '1m',  target: 20 },
    { duration: '2m',  target: 20 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 0  },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed:   ['rate<0.05'],
    stats_latency:     ['p(99)<3000'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/internal/stats`, {
    headers: { 'Accept': 'application/json', 'X-Monitor-Token': 'sora-monitor-2025' },
  });
  statsLatency.add(res.timings.duration);

  let body = null;
  try {
    body = JSON.parse(res.body);
  } catch (e) {
    console.error(`파싱 실패 - status: ${res.status} | body: ${res.body?.slice(0, 100)}`);
    requestsFailed.add(1);
    sleep(1);
    return;
  }

  const ok = check(res, {
    'status 200':           (r) => r.status === 200,
    'has cpuUsage':         () => body.cpuUsage !== undefined,
    'has hikari_active':    () => body.hikari_active !== undefined,
    'hikari_pending < 5':   () => body.hikari_pending < 5,
  });

  if (!ok) requestsFailed.add(1);

  if (__ITER % 10 === 0) {
    console.log(`[iter ${__ITER}] CPU: ${body.cpuUsage}% | MEM: ${body.memoryUsage}% | ` +
                `HikariCP: ${body.hikari_active}/${body.hikari_total} active, ${body.hikari_pending} pending`);
  }

  sleep(1);
}
