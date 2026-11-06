import { Navigate, Outlet, useLocation } from 'react-router'
import { Loading } from '../components/Loading'
import { hasAnyRole, type Role } from './roles'
import { useAuth } from './useAuth'

// UX only: hides screens a user can't use. The backend's authorization checks
// remain the real enforcement; never rely on these guards for security.

export function RequireAuth() {
  const { status, error } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <Loading label="Checking your session…" />
  if (status === 'error') {
    return (
      <p role="alert">
        We couldn't reach Colegio Connect. Please try again later.
        {error && ` (${error.message})`}
      </p>
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
