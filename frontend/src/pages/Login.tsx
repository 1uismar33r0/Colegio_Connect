import { useLocation } from 'react-router'

// Placeholder: the real sign-in form arrives with Issue #14.
export function Login() {
  const from = (useLocation().state as { from?: string } | null)?.from
  return (
    <>
      <h1>Sign in</h1>
      <p>The sign-in form arrives with Issue #14.</p>
      {from && <p>You&apos;ll return to {from} after signing in.</p>}
    </>
  )
}
