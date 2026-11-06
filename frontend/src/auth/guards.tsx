import { Navigate, Outlet, useLocation } from 'react-router'
import { ErrorMessage } from '../components/ErrorMessage'
import { Loading } from '../components/Loading'
import { hasAnyRole, type Role } from './roles'
import { useAuth } from './useAuth'

// UX only: hides screens a user can't use. The backend's authorization checks
// remain the real enforcement; never rely on these guards for security.

export function RequireAuth() {
  const { status, refresh } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <Loading label="Checking your session…" />
  if (status === 'error') {
    // Plain-language message only; technical details stay out of the UI.
    return (
      <ErrorMessage
        title="We couldn't reach Colegio Connect"
        message="Check your connection and try again."
        onRetry={() => void refresh()}
      />
    )
  }
  if (status === 'anonymous') {
    // Remember where they were going so #14's login can send them back.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

export function RequireRole({ roles }: { roles: readonly Role[] }) {
  const { roles: userRoles } = useAuth()
  if (!hasAnyRole(userRoles, roles)) return <Navigate to="/forbidden" replace />
  return <Outlet />
}
