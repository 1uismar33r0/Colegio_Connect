import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { resend, verify } from '../api/auth'
import { useAuthFlowState, type AuthFlowState } from '../auth/flowState'
import {
  ValidationFailure,
  describeFailure,
  useFormSubmit,
  type Failure,
} from '../auth/useFormSubmit'
import {
  codeError,
  collectErrors,
  emailError,
  normalizeCode,
  normalizeEmail,
} from '../auth/validation'
import { ErrorMessage } from '../components/ErrorMessage'
import { FormField } from '../components/FormField'
import { Notice } from '../components/Notice'

/** Matches RegistrationService.RESEND_COOLDOWN on the backend. */
export const RESEND_COOLDOWN_SECONDS = 60

export function Verify() {
  const navigate = useNavigate()
  const flow = useAuthFlowState()
  const [email, setEmail] = useState(flow.email ?? '')
  const { busy, failure, setFailure, formRef, run } = useFormSubmit()
  const errors = failure?.fieldErrors ?? {}

  const [notice, setNotice] = useState(flow.notice)
  const [resending, setResending] = useState(false)
  const [resendFailure, setResendFailure] = useState<Failure | null>(null)
  // A code was just sent when we arrive from registration, so start cooling down.
  const [cooldown, setCooldown] = useState(flow.notice ? RESEND_COOLDOWN_SECONDS : 0)

  useEffect(() => {
    if (cooldown <= 0) return
    const timer = setTimeout(() => setCooldown((s) => s - 1), 1000)
    return () => clearTimeout(timer)
  }, [cooldown])

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (busy) return
    const form = new FormData(e.currentTarget)
    const normalizedEmail = normalizeEmail(email)
    const code = normalizeCode(String(form.get('code') ?? ''))
    setResendFailure(null)

    void run(async () => {
      const invalid = collectErrors({ email: emailError(normalizedEmail), code: codeError(code) })
      if (Object.keys(invalid).length) throw new ValidationFailure(invalid)

      await verify(normalizedEmail, code)
      const state: AuthFlowState = {
        email: normalizedEmail,
        notice: 'Your email is verified. Sign in to continue.',
      }
      navigate('/login', { state })
    })
  }

  async function onResend() {
    if (resending || cooldown > 0) return
    const normalizedEmail = normalizeEmail(email)
    const invalid = emailError(normalizedEmail)
    setNotice(undefined)
    setResendFailure(null)
    if (invalid) {
      // Reuse the form's failure so the email field is marked and focused.
      setFailure({
        message: 'Enter your email to get a new code.',
        fieldErrors: { email: invalid },
      })
      return
    }
    setFailure(null)
    setResending(true)
    try {
      await resend(normalizedEmail)
      // The backend answers the same way for unknown or verified emails, so we do too.
      setNotice(
        `If ${normalizedEmail} still needs verifying, we requested a new code for it. It should arrive within a few minutes.`,
      )
      setCooldown(RESEND_COOLDOWN_SECONDS)
    } catch (err) {
      const described = describeFailure(err)
      setResendFailure(described)
      // 429 means the backend's own cooldown or hourly limit is still running.
      if (described.status === 429) setCooldown(RESEND_COOLDOWN_SECONDS)
    } finally {
      setResending(false)
    }
  }

  const resendBlocked = resending || cooldown > 0
  const resendLabel = resending
    ? 'Sending…'
    : cooldown > 0
      ? `Resend code (available in ${cooldown}s)`
      : 'Resend code'

  return (
    <form ref={formRef} onSubmit={onSubmit} noValidate className="auth-form">
      <h1>Verify your email</h1>
      <p>Enter the 6-digit code we emailed you. Codes expire after 10 minutes.</p>

      {notice && <Notice>{notice}</Notice>}
      {failure && <ErrorMessage title="We couldn't verify your email" message={failure.message} />}
      {resendFailure && (
        <ErrorMessage title="We couldn't send a new code" message={resendFailure.message} />
      )}

      <FormField
        label="Email"
        name="email"
        type="email"
        autoComplete="email"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={errors.email}
      />
      <FormField
        label="Verification code"
        name="code"
        type="text"
        inputMode="numeric"
        autoComplete="one-time-code"
        hint="6 digits. If you can't find the email, check your spam folder."
        required
        error={errors.code}
      />

      <div className="form-actions">
        <button type="submit" aria-disabled={busy}>
          {busy ? 'Verifying…' : 'Verify email'}
        </button>
        <button
          type="button"
          className="secondary"
          aria-disabled={resendBlocked}
          onClick={() => void onResend()}
        >
          {resendLabel}
        </button>
      </div>
      <p>
        Already verified? <Link to="/login">Sign in</Link>
      </p>
    </form>
  )
}
