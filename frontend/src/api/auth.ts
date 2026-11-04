export type AuthenticatedUser = {
  id: string
  email: string
  accountType: string
}

export type Credentials = { email: string; password: string }

export type ProblemDetail = {
  status?: number
  title?: string
  detail?: string
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail || problem.title || 'The request could not be completed.')
    this.problem = problem
  }
}

function csrfToken(): string | undefined {
  return document.cookie.split('; ').find((cookie) => cookie.startsWith('XSRF-TOKEN='))?.split('=')[1]
}

async function problemFrom(response: Response): Promise<ProblemDetail> {
  if ((response.headers.get('content-type') || '').includes('json')) return response.json() as Promise<ProblemDetail>
  return { status: response.status, detail: response.statusText }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const method = init.method?.toUpperCase() || 'GET'
  if (init.body) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = csrfToken()
    if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
  }
  const response = await fetch(path, { ...init, credentials: 'include', headers })
  if (!response.ok) throw new ApiError(await problemFrom(response))
  if (response.status === 204 || !(response.headers.get('content-type') || '').includes('json')) return undefined as T
  return response.json() as Promise<T>
}

export function initializeCsrf(): Promise<void> { return request<void>('/api/auth/csrf') }
export function currentUser(): Promise<AuthenticatedUser> { return request<AuthenticatedUser>('/api/auth/me') }
export function register(credentials: Credentials): Promise<void> { return request<void>('/api/auth/register', { method: 'POST', body: JSON.stringify(credentials) }) }
export function verify(email: string, code: string): Promise<void> { return request<void>('/api/auth/verify', { method: 'POST', body: JSON.stringify({ email, code }) }) }
export function resend(email: string): Promise<void> { return request<void>('/api/auth/resend', { method: 'POST', body: JSON.stringify({ email }) }) }
export async function login(credentials: Credentials): Promise<AuthenticatedUser> {
  const user = await request<AuthenticatedUser>('/api/auth/login', { method: 'POST', body: JSON.stringify(credentials) })
  await initializeCsrf()
  return user
}
export async function logout(): Promise<void> { await request<void>('/api/auth/logout', { method: 'POST' }); await initializeCsrf() }
