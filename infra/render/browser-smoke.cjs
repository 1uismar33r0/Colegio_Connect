// Optional isolated local browser check. Install playwright-core in a scratch
// directory and point PLAYWRIGHT_CORE_DIR to its package directory. No project
// dependency or browser binary is installed by this script.
const path = require('node:path')

const base = process.argv[2]
const chromePath = process.env.CHROME_PATH
const packageDir = process.env.PLAYWRIGHT_CORE_DIR
if (!base || !chromePath || !packageDir) {
  throw new Error('Pass local URL, CHROME_PATH and PLAYWRIGHT_CORE_DIR')
}
const parsed = new URL(base)
if (!['localhost', '127.0.0.1'].includes(parsed.hostname)) {
  throw new Error('Browser smoke is restricted to an explicit loopback URL')
}
const { chromium } = require(path.join(packageDir, 'index.js'))

;(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: chromePath })
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
    const errors = []
    page.on('pageerror', (error) => errors.push(error.message))
    await page.goto(base, { waitUntil: 'networkidle' })
    await page.getByRole('heading', { name: 'Colegio Connect' }).waitFor()
    await page.goto(new URL('/dashboard', base).href, { waitUntil: 'networkidle' })
    await page.getByRole('heading', { name: 'Sign in' }).waitFor()
    if (!page.url().endsWith('/login')) throw new Error('Signed-out dashboard did not redirect')

    if (process.env.SYNTHETIC_EMAIL && process.env.SYNTHETIC_PASSWORD) {
      await page.getByLabel('Email').fill(process.env.SYNTHETIC_EMAIL)
      await page.getByLabel('Password').fill(process.env.SYNTHETIC_PASSWORD)
      await page.getByRole('button', { name: 'Sign in' }).click()
      await page.waitForURL(/\/(dashboard|student)(?:\/|$)/)
      await page.reload({ waitUntil: 'networkidle' })
      if (page.url().endsWith('/login')) throw new Error('Session did not survive refresh')
      await page.goto(new URL('/student', base).href, { waitUntil: 'networkidle' })
      await page.getByRole('heading', { name: 'Find your next opportunity' }).waitFor()
      const mobile = await browser.newPage({ viewport: { width: 375, height: 812 } })
      await mobile.goto(base, { waitUntil: 'networkidle' })
      const width = await mobile.evaluate(() => document.documentElement.scrollWidth)
      if (width > 375) throw new Error(`Mobile viewport overflows: ${width}px`)
      await mobile.close()
    }
    if (errors.length) throw new Error(`Browser page errors: ${errors.join('; ')}`)
    console.log(`PASS local Chrome browser smoke at ${parsed.origin}; authenticated=${Boolean(process.env.SYNTHETIC_EMAIL)}`)
  } finally {
    await browser.close()
  }
})().catch((error) => { console.error(error.message); process.exitCode = 1 })
