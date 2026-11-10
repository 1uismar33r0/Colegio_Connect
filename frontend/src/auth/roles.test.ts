import { describe, expect, it } from 'vitest'
import { ROLES, hasAnyRole, isAdmin, rolesOf } from './roles'

describe('rolesOf', () => {
  it('returns [] when /me has no roles field (current backend)', () => {
    expect(rolesOf({ id: '1', email: 'a@upr.edu' })).toEqual([])
  })

  it('returns [] for null/undefined users', () => {
    expect(rolesOf(null)).toEqual([])
    expect(rolesOf(undefined)).toEqual([])
  })

  it('keeps known roles and drops unknown values', () => {
    expect(rolesOf({ roles: ['STUDENT', 'ADMIN', 42, 'MODERATOR'] })).toEqual([
      'STUDENT',
      'MODERATOR',
    ])
  })
})

describe('hasAnyRole', () => {
  it('is true when any required role is held', () => {
    expect(hasAnyRole(['STUDENT'], ['COMPANY', 'STUDENT'])).toBe(true)
  })

  it('is false when none are held', () => {
    expect(hasAnyRole([], ['STUDENT'])).toBe(false)
    expect(hasAnyRole(['STUDENT', 'COMPANY'], ['INSTITUTIONAL'])).toBe(false)
  })

  it('is always true for the administrator (MODERATOR has all access)', () => {
    for (const role of ROLES) expect(hasAnyRole(['MODERATOR'], [role])).toBe(true)
  })
})

describe('isAdmin', () => {
  it('is true only with the MODERATOR role', () => {
    expect(isAdmin(['STUDENT', 'MODERATOR'])).toBe(true)
    expect(isAdmin(['STUDENT', 'INSTITUTIONAL', 'COMPANY'])).toBe(false)
  })
})
