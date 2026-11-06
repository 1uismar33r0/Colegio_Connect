import { Link } from 'react-router'

export function Forbidden() {
  return (
    <>
      <h1>Access denied</h1>
      <p>Your account doesn&apos;t have permission to view that page.</p>
      <Link to="/dashboard">Back to your dashboard</Link>
    </>
  )
}
