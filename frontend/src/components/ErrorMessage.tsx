type Props = {
  title?: string
  message?: string
  onRetry?: () => void
}

export function ErrorMessage({ title = 'Something went wrong', message, onRetry }: Props) {
  return (
    <div className="error-box" role="alert">
      <h2>{title}</h2>
      {message && <p>{message}</p>}
      {onRetry && (
        <button type="button" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}
