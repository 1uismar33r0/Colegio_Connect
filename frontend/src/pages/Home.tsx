import { Link } from 'react-router'

export function Home() {
  return (
    <>
      <h1>Colegio Connect</h1>
      <p>Connecting UPRM students with capstone projects and organizations.</p>
      <Link to="/dashboard">Go to your dashboard</Link>
    </>
  )
}
