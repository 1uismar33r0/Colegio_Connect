import axe from 'axe-core'
import { ApiError, type ProblemDetail } from '../api/problem'

export function apiError(status: number, problem: ProblemDetail = {}): ApiError {
  return new ApiError(status, { status, ...problem })
}

/** axe run with the same settings as AppLayout.test.tsx. */
export async function axeViolations() {
  // jsdom can't render colors; contrast is verified in a real browser instead.
  const results = await axe.run(document.body, {
    rules: { 'color-contrast': { enabled: false } },
  })
  return results.violations
}
