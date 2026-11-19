import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const failures = new Rate('http_failures');
const latency = new Trend('request_latency', true);
const baseUrl = __ENV.BASE_URL || 'http://web';
const headers = __ENV.LOAD_TOKEN
  ? { Authorization: `Bearer ${__ENV.LOAD_TOKEN}`, Accept: 'application/json' }
  : { Accept: 'application/json' };

http.setResponseCallback(http.expectedStatuses(200, 401));

export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || '2m',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}/api/exhibition_templates`, { headers, tags: { endpoint: 'exhibition_templates' } });
  latency.add(response.timings.duration);
  const ok = check(response, {
    'API responds (2xx or expected auth)': (r) => (r.status >= 200 && r.status < 300) || r.status === 401,
  });
  failures.add(!ok);
  sleep(1);
}
