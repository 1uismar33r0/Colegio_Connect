export type ProblemDetail = {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: Record<string, string>
  /** Stable machine-readable reason, e.g. login 403s: EMAIL_NOT_VERIFIED, ACCOUNT_SUSPENDED. */
  code?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail || problem.title || 'The request could not be completed.')
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export function toApiError(response: Response, body: unknown): ApiError {
  const problem =
    body && typeof body === 'object' ? (body as ProblemDetail) : { status: response.status }
  return new ApiError(response.status, problem)
}
