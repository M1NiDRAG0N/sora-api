/**
 * 02-community.js - 커뮤니티 부하 테스트 (인증 필요)
 * 실행: k6 run 02-community.js -e BASE_URL=http://soragodong.duckdns.org -e TEST_EMAIL=xxx -e TEST_PASSWORD=xxx
 *
 * BoardDto 필드: boardIdx, userIdx, profileIdx, boardCategory, boardTitle,
 *               boardContent, userNickname, isUse, isLiked, likeCount,
 *               viewCount, createdAt, updatedAt, fileGrpIdx, replyCount
 */

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

const BASE_URL   = __ENV.BASE_URL      || 'http://soragodong.duckdns.org';
const TEST_EMAIL = __ENV.TEST_EMAIL    || 'test@example.com';
const TEST_PASS  = __ENV.TEST_PASSWORD || 'password';

const listLatency   = new Trend('community_list_latency');
const detailLatency = new Trend('community_detail_latency');
const errorRate     = new Rate('error_rate');

export const options = {
  scenarios: {
    browsing_users: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '2m',  target: 30 },
        { duration: '1m',  target: 30 },
        { duration: '30s', target: 0  },
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
  console.log('로그인 성공');
  return { sessionCookie };
}

export default function ({ sessionCookie }) {
  const jar = http.cookieJar();
  jar.set(BASE_URL, 'SESSION', sessionCookie);

  // 커뮤니티 목록 조회 - 응답: BoardDto[]
  const listRes = http.get(`${BASE_URL}/community/list?page=1`, {
    headers: { 'Accept': 'application/json' },
  });
  listLatency.add(listRes.timings.duration);

  let boards = [];
  try {
    boards = JSON.parse(listRes.body);
  } catch (e) {
    console.error(`목록 파싱 실패 - status: ${listRes.status}`);
    errorRate.add(true);
    sleep(2);
    return;
  }

  const listOk = check(listRes, {
    'list status 200':  (r) => r.status === 200,
    'list is array':    () => Array.isArray(boards),
    'list not empty':   () => boards.length > 0,
  });
  errorRate.add(!listOk);

  if (!listOk || boards.length === 0) {
    sleep(2);
    return;
  }

  // BoardDto.boardIdx 로 상세 조회
  const boardIdx = boards[0].boardIdx;
  sleep(1);

  const detailRes = http.get(`${BASE_URL}/community/view/${boardIdx}`, {
    headers: { 'Accept': 'application/json' },
  });
  detailLatency.add(detailRes.timings.duration);

  let detail = null;
  try {
    detail = JSON.parse(detailRes.body);
  } catch (e) {
    errorRate.add(true);
    sleep(2);
    return;
  }

  const detailOk = check(detailRes, {
    'detail status 200':     (r) => r.status === 200,
    'detail has boardIdx':   () => detail?.boardIdx !== undefined,
  });
  errorRate.add(!detailOk);

  if (__ITER % 50 === 0) {
    const statsRes = http.get(`${BASE_URL}/api/internal/stats`, {
      headers: { 'Accept': 'application/json' },
    });
    if (statsRes.status === 200) {
      const s = JSON.parse(statsRes.body);
      console.log(`[iter ${__ITER}] CPU: ${s.cpuUsage}% | MEM: ${s.memoryUsage}% | ` +
                  `HikariCP: ${s.hikari_active}/${s.hikari_total} active, ${s.hikari_pending} pending`);
    }
  }

  sleep(Math.random() * 2 + 1);
}
