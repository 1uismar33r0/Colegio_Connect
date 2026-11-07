import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, initCsrf, logout as apiLogout, unwrap } from '../api/client'
import { ApiError } from '../api/problem'
import { AuthContext, type AuthState } from './AuthContext'
import { rolesOf } from './roles'

const ANONYMOUS: AuthState = { status: 'anonymous', user: null, roles: [], error: null }

/** Asks the backend who is signed in. Never throws; always resolves to a state. */
async function loadSession(): Promise<AuthState> {
  try {
    await initCsrf()
    const user = await unwrap(api.GET('/api/auth/me'))
    return { status: 'authenticated', user, roles: rolesOf(user), error: null }
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return ANONYMOUS
    const error = err instanceof Error ? err : new Error(String(err))
    return { ...ANONYMOUS, status: 'error', error }
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ ...ANONYMOUS, status: 'loading' })

  // Restore the session once on mount; ignore the result if we unmounted first.
  useEffect(() => {
    let cancelled = false
    void loadSession().then((next) => {
      if (!cancelled) setState(next)
    })
    return () => {
      cancelled = true
    }
  }, [])

  // For event handlers (e.g. after login in #14).
  const refresh = useCallback(async () => {
    setState({ ...ANONYMOUS, status: 'loading' })
    setState(await loadSession())
  }, [])

  const logout = useCallback(async () => {
    await apiLogout()
    setState(ANONYMOUS)
    // await initCsrf() // the new session needs a fresh token, not needed because apiLogout() already does this
  }, [])

  const value = useMemo(() => ({ ...state, refresh, logout }), [state, refresh, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
