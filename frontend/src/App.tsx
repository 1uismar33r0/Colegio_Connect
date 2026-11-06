import { useAuth } from './auth/useAuth'

function App() {
  const { status, user, roles, error } = useAuth()
  return (
    <main>
      <h1>Colegio Connect</h1>
      <p role="status">
        Session: {status}
        {user && ` as ${user.email} (${user.accountType}); roles: ${roles.join(', ') || 'none'}`}
        {error && ` — ${error.message}`}
      </p>
    </main>
  )
}

export default App
