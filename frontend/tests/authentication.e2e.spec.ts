import { expect, request, test } from '@playwright/test'

const evidence = '../docs/testing/evidence/2026-11-03-real-browser-auth'

async function verificationCode(email: string): Promise<string> {
  const messages = await fetch('http://127.0.0.1:18025/api/v1/messages?limit=100').then((response) => response.json()) as { messages?: Array<{ ID?: string }> }
  const message = messages.messages?.find((candidate) => JSON.stringify(candidate).includes(email))
  if (!message?.ID) throw new Error(`Mailpit did not return a verification message for ${email}`)
  const detail = await fetch(`http://127.0.0.1:18025/api/v1/message/${message.ID}`).then((response) => response.text())
  const code = detail.match(/\b\d{6}\b/)?.[0]
  if (!code) throw new Error(`Mailpit message ${message.ID} did not contain a six-digit code`)
  return code
}

test('real browser registration, verification, session restoration, and logout', async ({ page }) => {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 10_000)}`
  const email = `playwright-${suffix}@upr.edu`
  const password = `Valid-${suffix}-password`
  const consoleErrors: string[] = []
  page.on('console', (entry) => { if (entry.type() === 'error') consoleErrors.push(entry.text()) })

  await page.goto('/')
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()

  await page.getByRole('button', { name: 'Need an account? Register' }).click()
  await page.getByLabel('Email').fill('not-an-institutional-email@example.com')
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page.getByRole('alert')).toContainText('Only UPRM institutional emails')

  await page.getByRole('textbox', { name: 'Email' }).fill(email)
  const registration = page.waitForResponse((response) => response.url().endsWith('/api/auth/register') && response.request().method() === 'POST')
  await page.getByRole('button', { name: 'Create account' }).click()
  expect((await registration).status()).toBe(201)
  await expect(page.getByRole('heading', { name: 'Verify your email' })).toBeVisible()
  await page.screenshot({ path: `${evidence}/registration-awaiting-verification.png`, fullPage: true })

  const code = await verificationCode(email)
  const invalidCode = code === '000000' ? '000001' : '000000'
  await page.getByRole('textbox', { name: 'Email' }).fill(email)
  await page.getByLabel('Verification code').fill(invalidCode)
  await page.getByRole('button', { name: 'Verify email' }).click()
  await expect(page.getByRole('alert')).toContainText('Invalid or expired code')

  await page.getByLabel('Verification code').fill(code)
  const verification = page.waitForResponse((response) => response.url().endsWith('/api/auth/verify') && response.request().method() === 'POST')
  await page.getByRole('button', { name: 'Verify email' }).click()
  expect((await verification).status()).toBe(204)
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()

  await page.getByRole('textbox', { name: 'Email' }).fill(email)
  await page.getByLabel('Password').fill('wrong-password')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('alert')).toContainText('Invalid email or password')

  const csrfFailure = await page.evaluate(async () => (await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })).status)
  expect(csrfFailure).toBe(403)

  await page.getByLabel('Password').fill(password)
  const signedIn = page.waitForResponse((response) => response.url().endsWith('/api/auth/login') && response.request().method() === 'POST')
  await page.getByRole('button', { name: 'Sign in' }).click()
  expect((await signedIn).status()).toBe(200)
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible()
  await expect(page.getByText(`You are signed in as ${email}.`)).toBeVisible()
  const oldSession = (await page.context().cookies()).find((cookie) => cookie.name === 'JSESSIONID')
  expect(oldSession?.value).toBeTruthy()
  expect(oldSession).toMatchObject({ secure: true, httpOnly: true, sameSite: 'Lax' })
  await page.screenshot({ path: `${evidence}/authenticated-session.png`, fullPage: true })

  const me = await page.evaluate(async () => (await fetch('/api/auth/me', { credentials: 'include' })).status)
  expect(me).toBe(200)
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible()

  const logout = page.waitForResponse((response) => response.url().endsWith('/api/auth/logout') && response.request().method() === 'POST')
  await page.getByRole('button', { name: 'Sign out' }).click()
  expect((await logout).status()).toBe(204)
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
  await page.screenshot({ path: `${evidence}/after-logout.png`, fullPage: true })

  const staleClient = await request.newContext({ baseURL: 'http://127.0.0.1:5173', extraHTTPHeaders: { Cookie: `JSESSIONID=${oldSession?.value}` } })
  const staleMe = await staleClient.get('/api/auth/me')
  expect(staleMe.status()).toBe(401)
  await staleClient.dispose()
  const unexpectedConsoleErrors = consoleErrors.filter((entry) => !entry.startsWith('Failed to load resource: the server responded with a status of'))
  expect(unexpectedConsoleErrors).toEqual([])
})
