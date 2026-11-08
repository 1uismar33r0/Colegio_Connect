import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { register } from '../api/auth'
import type { AuthFlowState } from '../auth/flowState'
import { ValidationFailure, useFormSubmit } from '../auth/useFormSubmit'
import { collectErrors, emailError, newPasswordError, normalizeEmail } from '../auth/validation'
import { ErrorMessage } from '../components/ErrorMessage'
import { FormField } from '../components/FormField'

export function Register() {
  const navigate = useNavigate()
  const { busy, failure, formRef, run } = useFormSubmit()
  const errors = failure?.fieldErrors ?? {}

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (busy) return
    const form = new FormData(e.currentTarget)
    const email = normalizeEmail(String(form.get('email') ?? ''))
    const password = String(form.get('password') ?? '')
    const confirm = String(form.get('confirmPassword') ?? '')

    void run(async () => {
      const invalid = collectErrors({
        email: emailError(email, { requireUprm: true }),
        password: newPasswordError(password),
        confirmPassword: confirm === password ? undefined : 'The passwords do not match.',
      })
      if (Object.keys(invalid).length) throw new ValidationFailure(invalid)

      await register(email, password)
      const state: AuthFlowState = {
        email,
        notice: `We sent a 6-digit code to ${email}. It expires in 10 minutes.`,
      }
      navigate('/verify', { state })
    })
  }

  return (
    <form ref={formRef} onSubmit={onSubmit} noValidate className="auth-form">
      <h1>Create your account</h1>
      <p>Colegio Connect is open to UPRM students and staff with an institutional email.</p>

      {failure && (
        <ErrorMessage title="We couldn't create your account" message={failure.message} />
      )}
      {failure?.status === 409 && (
        <p>
          Already registered? <Link to="/login">Sign in</Link> or{' '}
          <Link to="/verify">enter your verification code</Link>.
        </p>
      )}

      <FormField
        label="Email"
        name="email"
        type="email"
        autoComplete="email"
        hint="Use your @upr.edu address."
        required
        error={errors.email}
      />
      <FormField
        label="Password"
        name="password"
        type="password"
        autoComplete="new-password"
        hint="8 to 64 characters."
        required
        error={errors.password}
      />
      <FormField
        label="Confirm password"
        name="confirmPassword"
        type="password"
        autoComplete="new-password"
        required
        error={errors.confirmPassword}
      />

      {/* aria-disabled (not disabled) keeps keyboard focus on the button while busy. */}
      <button type="submit" aria-disabled={busy}>
        {busy ? 'Creating account…' : 'Create account'}
      </button>
      <p>
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </form>
  )
}
