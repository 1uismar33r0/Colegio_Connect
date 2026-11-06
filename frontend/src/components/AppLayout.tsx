import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { hasAnyRole, type Role } from '../auth/roles'

const AREAS: { to: string; label: string; role: Role }[] = [
  { to: '/student', label: 'Student', role: 'STUDENT' },
  { to: '/institution', label: 'Institution', role: 'INSTITUTIONAL' },
  { to: '/company', label: 'Company', role: 'COMPANY' },
  { to: '/moderation', label: 'Moderation', role: 'MODERATOR' },
]

export function AppLayout() {
  const { status, roles, logout } = useAuth()
  const visibleAreas = AREAS.filter((a) => hasAnyRole(roles, [a.role]))

  return (
    <>
      <header>
        <NavLink to="/">Colegio Connect</NavLink>
        <nav aria-label="Primary">
          <ul>
            {status === 'authenticated' && (
              <li>
                <NavLink to="/dashboard">Dashboard</NavLink>
              </li>
            )}
            {visibleAreas.map((a) => (
              <li key={a.to}>
                <NavLink to={a.to}>{a.label}</NavLink>
              </li>
            ))}
          </ul>
        </nav>
        {status === 'authenticated' ? (
          <button type="button" onClick={() => void logout()}>
            Sign out
          </button>
        ) : (
          <NavLink to="/login">Sign in</NavLink>
        )}
      </header>
      <main id="main">
        <Outlet />
      </main>
    </>
  )
}
