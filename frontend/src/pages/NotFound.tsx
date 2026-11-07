import { Link } from 'react-router'

export function NotFound() {
  return (
    <>
      <h1>Page not found</h1>
      <p>That address doesn&apos;t exist.</p>
      <Link to="/">Go home</Link>
    </>
  )
}
