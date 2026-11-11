// LOCAL ONLY: run with APP_REGISTRATION_ENABLED=true in the isolated Docker test app.
// This reads a synthetic verification code inside the container and never prints it.
import { execFileSync } from 'node:child_process'

const base = process.argv[2]
const container = process.argv[3]
if (!base || !container) throw new Error('Pass local app URL and isolated app container name')
if (!['localhost', '127.0.0.1'].includes(new URL(base).hostname)) {
  throw new Error('Synthetic mail smoke is restricted to an explicit loopback URL')
}
const email = process.env.SYNTHETIC_EMAIL ?? `synthetic.${Date.now()}@upr.edu`
const password = 'SyntheticOnly123!'
let cookies = []
let token = ''

function absorb(response) {
  for (const line of response.headers.getSetCookie()) {
    const pair = line.split(';')[0]
    cookies = [...cookies.filter((c) => !c.startsWith(pair.split('=')[0] + '=')), pair]
    if (pair.startsWith('XSRF-TOKEN=')) token = decodeURIComponent(pair.split('=')[1])
  }
}

async function request(path, method = 'GET', body) {
  const response = await fetch(new URL(path, base), {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(cookies.length ? { Cookie: cookies.join('; ') } : {}),
      ...(method !== 'GET' ? { 'X-XSRF-TOKEN': token } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  absorb(response)
  return response
}

function inbox(path) {
  const output = execFileSync('docker', ['exec', container, 'wget', '-qO-',
    `http://127.0.0.1:8025${path}`], { encoding: 'utf8' })
  return JSON.parse(output)
}

let response = await request('/api/auth/csrf')
if (response.status !== 204 || !token) throw new Error('CSRF bootstrap failed')
response = await request('/api/auth/register', 'POST', { email, password })
if (response.status !== 201) throw new Error(`Synthetic registration: ${response.status}`)

let message
for (let attempt = 0; attempt < 20; attempt++) {
  const list = inbox('/api/v1/messages')
  const item = list.messages?.find((candidate) =>
    candidate.To?.some((recipient) => recipient.Address === email))
  if (item) { message = inbox(`/api/v1/message/${item.ID}`); break }
  await new Promise((resolve) => setTimeout(resolve, 500))
}
if (!message) throw new Error('Synthetic verification message not captured by private Mailpit')
const code = (message.Text ?? '').match(/\b\d{6}\b/)?.[0]
if (!code) throw new Error('Verification code missing from private synthetic message')

response = await request('/api/auth/verify', 'POST', { email, code })
if (response.status !== 204) throw new Error(`Synthetic verification: ${response.status}`)
response = await request('/api/auth/login', 'POST', { email, password })
if (response.status !== 200) throw new Error(`Verified synthetic login: ${response.status}`)
response = await request('/api/auth/me')
if (response.status !== 200) throw new Error(`Session restoration: ${response.status}`)
response = await request('/api/admin/audit-events')
if (response.status !== 403) throw new Error(`Student-only admin denial: ${response.status}`)
response = await request('/api/auth/resend', 'POST', { email })
if (![202, 429].includes(response.status)) throw new Error(`Resend response: ${response.status}`)
console.log('PASS synthetic registration, private Mailpit capture, verification, login, session restore, role denial, resend response')
