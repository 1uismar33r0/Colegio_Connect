import { Link } from 'react-router'
import { AREAS } from '../auth/areas'
import { hasAnyRole } from '../auth/roles'
import { useAuth } from '../auth/useAuth'

export function Dashboard() {
  const { user, roles } = useAuth()
  const areas = AREAS.filter((a) => hasAnyRole(roles, [a.role]))

  return (
    <>
      <h1>Dashboard</h1>
      <p>Signed in as {user?.email}.</p>
      {areas.length === 0 ? (
        // Accounts register without a type; an administrator assigns roles afterwards.
        <p className="notice" role="status">
          Your account is ready. An administrator still needs to assign your role before you can use
          Colegio Connect's student, office, organization or moderation pages.
        </p>
      ) : (
        <section aria-labelledby="dashboard-areas-heading">
          <h2 id="dashboard-areas-heading">Your areas</h2>
          <ul>
            {areas.map((a) => (
              <li key={a.to}>
                <Link to={a.to}>{a.accountLabel}</Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  )
}
