import { useAuth } from '../auth/useAuth'

export function Dashboard() {
  const { user, roles } = useAuth()
  return (
    <>
      <h1>Dashboard</h1>
      <p>Signed in as {user?.email}.</p>
      <p>Roles: {roles.length ? roles.join(', ') : 'none assigned yet'}</p>
    </>
  )
}
