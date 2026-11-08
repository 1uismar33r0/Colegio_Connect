import { useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/problem'
import type { FieldErrors } from './validation'

export type Failure = {
  /** Shown in the form's alert box. */
  message: string
  fieldErrors: FieldErrors
  /** HTTP status, or undefined for client-side and network failures. */
  status?: number
}

/** Thrown inside a submit action when client-side validation fails. */
export class ValidationFailure extends Error {
  readonly fieldErrors: FieldErrors
  constructor(fieldErrors: FieldErrors) {
    super('Fix the highlighted fields and try again.')
    this.fieldErrors = fieldErrors
  }
}

/** Turns anything a submit can throw into plain-language text for the user. */
export function describeFailure(err: unknown): Failure {
  if (err instanceof ValidationFailure)
    return { message: err.message, fieldErrors: err.fieldErrors }
  if (err instanceof ApiError) {
    // Server errors stay generic; technical details never reach the UI.
    if (err.status >= 500) {
      return {
        message: 'Something went wrong on our side. Try again in a moment.',
        fieldErrors: {},
        status: err.status,
      }
    }
    const fieldErrors = err.problem.errors ?? {}
    const message = Object.keys(fieldErrors).length
      ? 'Fix the highlighted fields and try again.'
      : err.message
    return { message, fieldErrors, status: err.status }
  }
  return {
    message: "We couldn't reach Colegio Connect. Check your connection and try again.",
    fieldErrors: {},
  }
}

/**
 * Tracks the busy and failure state of a form submit. After a failure, focus
 * moves to the first invalid field so keyboard and screen reader users land
 * where the problem is; the form's alert box announces the summary.
 */
export function useFormSubmit() {
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<Failure | null>(null)
  const formRef = useRef<HTMLFormElement>(null)

  useEffect(() => {
    if (!failure) return
    formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
  }, [failure])

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setFailure(null)
    try {
      await action()
    } catch (err) {
      setFailure(describeFailure(err))
    } finally {
      setBusy(false)
    }
  }

  return { busy, failure, setFailure, formRef, run }
}
