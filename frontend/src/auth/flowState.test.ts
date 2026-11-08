import { describe, expect, it } from 'vitest'
import { safeReturnPath } from './flowState'

describe('safeReturnPath', () => {
  it.each(['/dashboard', '/student/projects', '/company/listings?tab=open'])(
    'keeps same-app path %s',
    (path) => {
      expect(safeReturnPath(path)).toBe(path)
    },
  )

  it.each([
    undefined,
    '',
    'dashboard',
    'https://evil.example',
    '//evil.example',
    '/\\evil.example',
    '\\\\evil.example',
    '/student\\..\\admin',
    '/ /evil.example',
    '/\t/evil.example',
    '/\n/evil.example',
    '/\u0000',
  ])('falls back to /dashboard for %j', (path) => {
    expect(safeReturnPath(path)).toBe('/dashboard')
  })
})
