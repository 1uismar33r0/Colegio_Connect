import { api, unwrap, type UserResponse } from './client'

// Thin wrappers over the typed client so pages stay small and tests can mock one module.

export function register(email: string, password: string): Promise<void> {
  return unwrap(api.POST('/api/auth/register', { body: { email, password } }))
}

export function verify(email: string, code: string): Promise<void> {
  return unwrap(api.POST('/api/auth/verify', { body: { email, code } }))
}

export function resend(email: string): Promise<void> {
  return unwrap(api.POST('/api/auth/resend', { body: { email } }))
}

export function login(email: string, password: string): Promise<UserResponse> {
  return unwrap(api.POST('/api/auth/login', { body: { email, password } }))
}
