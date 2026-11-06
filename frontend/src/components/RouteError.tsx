import { isRouteErrorResponse, Link, useRouteError } from 'react-router'

export function RouteError() {
  const error = useRouteError()
  // Show details only in development; never leak stack traces to users.
  if (import.meta.env.DEV) console.error(error)

  let title = 'Something went wrong'
  let message = 'An unexpected error occurred. Please reload the page.'
  if (isRouteErrorResponse(error)) {
    if (error.status === 404) {
      title = 'Page not found'
      message = "That address doesn't exist."
    } else {
      title = `Error ${error.status}`
      message = error.statusText || message
    }
  }

  return (
    <main id="main" className="container">
      <div role="alert">
        <h1>{title}</h1>
        <p>{message}</p>
      </div>
      <Link to="/">Go home</Link>
    </main>
  )
}
