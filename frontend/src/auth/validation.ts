// Client-side checks mirror the backend rules (RegisterRequest, VerifyRequest,
// RegistrationService) so users get instant feedback. The backend still validates.

export type FieldErrors = Record<string, string>

const UPRM_DOMAIN = '@upr.edu'
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+$/
const BCRYPT_MAX_BYTES = 72

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase()
}

export function emailError(email: string, { requireUprm = false } = {}): string | undefined {
  const value = normalizeEmail(email)
  if (!value) return 'Enter your email address.'
  if (!EMAIL_SHAPE.test(value)) return 'Enter a valid email address, like name@upr.edu.'
  if (requireUprm && !value.endsWith(UPRM_DOMAIN)) {
    return `Use your UPRM institutional email (${UPRM_DOMAIN}).`
  }
  return undefined
}

export function newPasswordError(password: string): string | undefined {
  if (!password) return 'Enter a password.'
  if (password.length < 8) return 'Use at least 8 characters.'
  if (password.length > 64 || new TextEncoder().encode(password).length > BCRYPT_MAX_BYTES) {
    return 'Use 64 characters or fewer.'
  }
  return undefined
}

/** Removes the spaces people often type or paste into a code. */
export function normalizeCode(code: string): string {
  return code.replace(/\s/g, '')
}

export function codeError(code: string): string | undefined {
  const value = normalizeCode(code)
  if (!value) return 'Enter the 6-digit code from your email.'
  if (!/^\d{6}$/.test(value)) return 'The code is 6 digits.'
  return undefined
}

/** Drops fields without an error, so an empty object means the form is valid. */
export function collectErrors(errors: Record<string, string | undefined>): FieldErrors {
  return Object.fromEntries(
    Object.entries(errors).filter((entry): entry is [string, string] => Boolean(entry[1])),
  )
}
