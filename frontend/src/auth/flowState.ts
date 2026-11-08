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

/** Only same-app paths are allowed as a post-login destination. */
export function safeReturnPath(from: string | undefined): string {
  return from && from.startsWith('/') && !from.startsWith('//') ? from : '/dashboard'
}
