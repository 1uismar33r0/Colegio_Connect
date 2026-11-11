import type { ReactNode } from 'react'

type Props = {
  title?: string
  message?: string
  onRetry?: () => void
  /** Extra actions, such as a sign-in link. */
  children?: ReactNode
}

export function ErrorMessage({
  title = 'Something went wrong',
  message,
  onRetry,
  children,
}: Props) {
  return (
    <div className="error-box" role="alert">
      <h2>{title}</h2>
      {message && <p>{message}</p>}
      {onRetry && (
        <button type="button" onClick={onRetry}>
          Try again
        </button>
      )}
      {children}
    </div>
  )
}
