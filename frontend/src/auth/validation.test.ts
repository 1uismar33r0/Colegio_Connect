import { describe, expect, it } from 'vitest'
import { codeError, collectErrors, emailError, newPasswordError, normalizeCode } from './validation'

describe('validation', () => {
  it('requires a UPRM email only when asked', () => {
    expect(emailError('')).toBe('Enter your email address.')
    expect(emailError('not-an-email')).toMatch(/valid email/)
    expect(emailError('someone@gmail.com')).toBeUndefined()
    expect(emailError('someone@gmail.com', { requireUprm: true })).toMatch(/@upr\.edu/)
    expect(emailError('  Name@UPR.EDU ', { requireUprm: true })).toBeUndefined()
  })

  it('enforces the backend password bounds, including the 72-byte bcrypt limit', () => {
    expect(newPasswordError('')).toBe('Enter a password.')
    expect(newPasswordError('1234567')).toMatch(/at least 8/)
    expect(newPasswordError('12345678')).toBeUndefined()
    expect(newPasswordError('a'.repeat(65))).toMatch(/64 characters/)
    // 40 two-byte characters: under 64 chars but 80 bytes.
    expect(newPasswordError('é'.repeat(40))).toMatch(/64 characters/)
  })

  it('accepts a 6-digit code with spaces', () => {
    expect(normalizeCode(' 123 456 ')).toBe('123456')
    expect(codeError('123 456')).toBeUndefined()
    expect(codeError('12345')).toBe('The code is 6 digits.')
    expect(codeError('abcdef')).toBe('The code is 6 digits.')
    expect(codeError('')).toMatch(/Enter the 6-digit code/)
  })

  it('collects only the fields that have errors', () => {
    expect(collectErrors({ email: 'bad', password: undefined })).toEqual({ email: 'bad' })
  })
})
