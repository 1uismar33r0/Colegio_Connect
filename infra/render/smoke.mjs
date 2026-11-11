// Run against the isolated local staging image: node infra/render/smoke.mjs http://127.0.0.1:18084
const base = process.argv[2]
if (!base || !/^https?:\/\//.test(base)) throw new Error('Pass the local staging base URL')
const checks = []

async function expect(path, expected, contentType) {
  const response = await fetch(new URL(path, base), { redirect: 'manual' })
  const actual = response.status
  const type = response.headers.get('content-type') ?? ''
  if (actual !== expected || (contentType && !type.includes(contentType))) {
    throw new Error(`${path}: status ${actual}, type ${type}; expected ${expected} ${contentType ?? ''}`)
  }
  checks.push(`${path}: ${actual}`)
  return response
}

for (const path of ['/', '/login', '/register', '/dashboard', '/student',
  '/student/responses', '/institution', '/company', '/moderation',
  '/moderation/accounts', '/moderation/audit']) {
  await expect(path, 200, 'text/html')
}
const root = await fetch(base)
const html = await root.text()
const asset = html.match(/(?:src|href)="(\/assets\/[^\"]+)"/)?.[1]
if (!asset) throw new Error('Vite asset URL not found in index.html')
await expect(asset, 200)
await expect('/assets/missing.js', 404)
await expect('/api/auth/me', 401)
await expect('/api/me/activity', 401)
await expect('/api/admin/audit-events', 401)
await expect('/api/not-a-route', 401)
await expect('/v3/api-docs', 404)
await expect('/actuator/health', 200, 'json')
await expect('/actuator/health/readiness', 200, 'json')

const payload = JSON.stringify({ email: 'render.disabled@upr.edu', password: 'SyntheticOnly123!' })
const noCsrf = await fetch(new URL('/api/auth/register', base), {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body: payload,
})
if (noCsrf.status !== 403) throw new Error(`CSRF-less registration: ${noCsrf.status}`)
checks.push('registration without CSRF: 403')
const csrf = await fetch(new URL('/api/auth/csrf', base))
if (csrf.status !== 204) throw new Error(`CSRF init: ${csrf.status}`)
const cookies = csrf.headers.getSetCookie()
const xsrf = cookies.find((cookie) => cookie.startsWith('XSRF-TOKEN='))?.split(';')[0]
if (!xsrf) throw new Error('CSRF cookie missing')
const session = cookies.find((cookie) => cookie.startsWith('JSESSIONID='))?.split(';')[0]
const registration = await fetch(new URL('/api/auth/register', base), {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    Cookie: [xsrf, session].filter(Boolean).join('; '),
    'X-XSRF-TOKEN': decodeURIComponent(xsrf.split('=')[1]),
  },
  body: payload,
})
if (registration.status !== 503) throw new Error(`Staging registration gate: ${registration.status}`)
checks.push('registration with CSRF: 503 (disabled)')
const requestHeaders = {
  'Content-Type': 'application/json',
  Cookie: [xsrf, session].filter(Boolean).join('; '),
  'X-XSRF-TOKEN': decodeURIComponent(xsrf.split('=')[1]),
}
const resend = await fetch(new URL('/api/auth/resend', base), {
  method: 'POST', headers: requestHeaders,
  body: JSON.stringify({ email: 'render.disabled@upr.edu' }),
})
if (resend.status !== 503) throw new Error(`Staging resend gate: ${resend.status}`)
checks.push('resend with CSRF: 503 (disabled)')
const verify = await fetch(new URL('/api/auth/verify', base), {
  method: 'POST', headers: requestHeaders,
  body: JSON.stringify({ email: 'render.disabled@upr.edu', code: '000000' }),
})
if (verify.status !== 400) throw new Error(`Unknown-account verify: ${verify.status}`)
checks.push('unknown-account verify: 400')
console.log(checks.join('\n'))
console.log(`PASS ${checks.length} HTTP checks`)
