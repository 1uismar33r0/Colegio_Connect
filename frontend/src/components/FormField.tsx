import { useId, type InputHTMLAttributes } from 'react'

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> & {
  label: string
  hint?: string
  error?: string
}

/** A labelled input whose hint and error are announced by screen readers. */
export function FormField({ label, hint, error, ...inputProps }: Props) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint && hintId, error && errorId].filter(Boolean).join(' ') || undefined

  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {inputProps.required && <span aria-hidden="true"> *</span>}
      </label>
      {hint && (
        <p id={hintId} className="hint">
          {hint}
        </p>
      )}
      <input
        {...inputProps}
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
      />
      {error && (
        <p id={errorId} className="field-error">
          {error}
        </p>
      )}
    </div>
  )
}
