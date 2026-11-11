import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { login } from '../api/auth'
import { ApiError } from '../api/problem'
import { safeReturnPath, useAuthFlowState, type AuthFlowState } from '../auth/flowState'
import { ValidationFailure, useFormSubmit } from '../auth/useFormSubmit'
import { useAuth } from '../auth/useAuth'
import { collectErrors, emailError, normalizeEmail } from '../auth/validation'
import { ErrorMessage } from '../components/ErrorMessage'
import { FormField } from '../components/FormField'
import { Notice } from '../components/Notice'

export function Login() {
  const { status, refresh } = useAuth()
  const navigate = useNavigate()
  const flow = useAuthFlowState()
  const returnTo = safeReturnPath(flow.from)
  const { busy, failure, formRef, run } = useFormSubmit()
  const errors = failure?.fieldErrors ?? {}

  // Already signed in (e.g. the back button): skip the form.
  if (status === 'authenticated' && !busy) return <Navigate to={returnTo} replace />

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (busy) return
    const form = new FormData(e.currentTarget)
    const email = normalizeEmail(String(form.get('email') ?? ''))
    const password = String(form.get('password') ?? '')

    void run(async () => {
      const invalid = collectErrors({
        email: emailError(email),
        password: password ? undefined : 'Enter your password.',
      })
      if (Object.keys(invalid).length) throw new ValidationFailure(invalid)

      try {
        await login(email, password)
      } catch (err) {
        // Unverified accounts get a 403 with a stable code; send them to finish verification.
        if (err instanceof ApiError && err.problem.code === 'EMAIL_NOT_VERIFIED') {
          const state: AuthFlowState = {
            email,
            notice:
              'Your email is not verified yet. Enter the code we sent you, or request a new one.',
          }
          navigate('/verify', { state })
          return
        }
        throw err
      }
      await refresh() // AuthProvider loads /me so the route guards see the session
      navigate(returnTo, { replace: true })
    })
  }

  return (
    <form ref={formRef} onSubmit={onSubmit} noValidate className="auth-form">
      <h1>Sign in</h1>
      {flow.notice && <Notice>{flow.notice}</Notice>}
      {flow.from && !flow.notice && <p>Sign in to continue to {flow.from}.</p>}
      {failure && <ErrorMessage title="We couldn't sign you in" message={failure.message} />}

      <FormField
        label="Email"
        name="email"
        type="email"
        autoComplete="email"
        defaultValue={flow.email}
        required
        error={errors.email}
      />
      <FormField
        label="Password"
        name="password"
        type="password"
        autoComplete="current-password"
        required
        error={errors.password}
      />

      <button type="submit" aria-disabled={busy}>
        {busy ? 'Signing in…' : 'Sign in'}
      </button>
      <p>
        New to Colegio Connect? <Link to="/register">Create an account</Link>
      </p>
      <p>
        Have a verification code? <Link to="/verify">Verify your email</Link>
      </p>
    </form>
  )
}
