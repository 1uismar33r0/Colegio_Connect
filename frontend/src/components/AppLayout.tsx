import { useEffect, useRef } from 'react'
import { NavLink, Outlet, useLocation, useMatches } from 'react-router'
import { AREAS, accountLabels, canOpenArea } from '../auth/areas'
import { useAuth } from '../auth/useAuth'

export type RouteHandle = { title?: string }

const APP_NAME = 'Colegio Connect'

export function AppLayout() {
  const { status, roles, user, logout } = useAuth()
  const visibleAreas = AREAS.filter((a) => canOpenArea(a, roles, user))

  // Page title: the deepest route that declares handle.title wins.
  const matches = useMatches()
  const pageTitle = [...matches]
    .reverse()
    .map((m) => (m.handle as RouteHandle | undefined)?.title)
    .find(Boolean)
  useEffect(() => {
    document.title = pageTitle ? `${pageTitle} · ${APP_NAME}` : APP_NAME
  }, [pageTitle])

  // After client-side navigation, move focus to <main> so screen readers
  // announce the new page instead of staying on the clicked link.
  const mainRef = useRef<HTMLElement>(null)
  const { pathname } = useLocation()
  const previousPath = useRef(pathname)
  useEffect(() => {
    if (previousPath.current !== pathname) {
      previousPath.current = pathname
      mainRef.current?.focus()
    }
  }, [pathname])

  return (
    <>
      <a className="skip-link" href="#main">
        Skip to main content
      </a>
      <header className="app-header">
        <NavLink to="/" className="brand">
          {APP_NAME}
        </NavLink>
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
        <div className="session">
          {status === 'authenticated' ? (
            <>
              {roles.length > 0 && (
                <span className="account-role">{accountLabels(roles).join(', ')}</span>
              )}
              <button type="button" onClick={() => void logout()}>
                Sign out
              </button>
            </>
          ) : (
            <NavLink to="/login">Sign in</NavLink>
          )}
        </div>
      </header>
      <main id="main" ref={mainRef} tabIndex={-1} className="container">
        <Outlet />
      </main>
      <footer className="app-footer">
        <p>{APP_NAME} · University of Puerto Rico at Mayagüez</p>
      </footer>
    </>
  )
}
