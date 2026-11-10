import { useLocation } from 'react-router'

/**
 * Router state passed between the register, verify and login screens.
 * The email travels in history state, not the URL, so it is not written to
 * server logs or shared links.
 */
export type AuthFlowState = {
  email?: string
  notice?: string
  /** Where RequireAuth was sending the user before it redirected to login. */
  from?: string
}

export function useAuthFlowState(): AuthFlowState {
  const state: unknown = useLocation().state
  return state && typeof state === 'object' ? (state as AuthFlowState) : {}
}

const DEFAULT_RETURN_PATH = '/dashboard'

/**
 * Only same-app paths are allowed as a post-login destination. Rejects
 * protocol-relative URLs ("//host"), any backslash (browsers treat "/\host"
 * like "//host"), and whitespace or control characters, which a router
 * pathname never contains.
 */
export function safeReturnPath(from: string | undefined): string {
  if (!from || !from.startsWith('/') || from.startsWith('//')) return DEFAULT_RETURN_PATH
  // eslint-disable-next-line no-control-regex -- control characters are exactly what we reject
  if (/[\\\s\u0000-\u001f\u007f]/.test(from)) return DEFAULT_RETURN_PATH
  return from
}
