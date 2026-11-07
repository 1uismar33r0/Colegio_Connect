import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { fakeAuth, renderWithAuth } from '../test/renderWithAuth'
import { RequireAuth, RequireRole } from './guards'

const routes = [
  { path: '/login', element: <h1>Login page</h1> },
  { path: '/forbidden', element: <h1>Forbidden page</h1> },
  {
    element: <RequireAuth />,
    children: [
      { path: '/dashboard', element: <h1>Dashboard page</h1> },
      {
        element: <RequireRole roles={['STUDENT']} />,
        children: [{ path: '/student', element: <h1>Student page</h1> }],
      },
    ],
  },
]

describe('RequireAuth', () => {
  it('shows a status message while the session loads', () => {
    renderWithAuth(routes, '/dashboard', fakeAuth({ status: 'loading' }))
    expect(screen.getByRole('status')).toHaveTextContent('Checking your session')
  })

  it('redirects anonymous users to /login and remembers where they were going', () => {
    const router = renderWithAuth(
      routes,
      '/dashboard',
      fakeAuth({ status: 'anonymous', user: null }),
    )
    expect(screen.getByRole('heading', { name: 'Login page' })).toBeInTheDocument()
    expect(router.state.location.state).toEqual({ from: '/dashboard' })
  })

  it('shows a retryable alert when the backend is unreachable', async () => {
    const refresh = vi.fn(async () => {})
    renderWithAuth(routes, '/dashboard', fakeAuth({ status: 'error', user: null, refresh }))
    expect(screen.getByRole('alert')).toHaveTextContent("couldn't reach Colegio Connect")
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(refresh).toHaveBeenCalledOnce()
  })

  it('renders the page for signed-in users', () => {
    renderWithAuth(routes, '/dashboard')
    expect(screen.getByRole('heading', { name: 'Dashboard page' })).toBeInTheDocument()
  })
})

describe('RequireRole', () => {
  it('sends users without the role to /forbidden', () => {
    renderWithAuth(routes, '/student', fakeAuth({ roles: [] }))
    expect(screen.getByRole('heading', { name: 'Forbidden page' })).toBeInTheDocument()
  })

  it('renders the page for users with the role', () => {
    renderWithAuth(routes, '/student', fakeAuth({ roles: ['STUDENT'] }))
    expect(screen.getByRole('heading', { name: 'Student page' })).toBeInTheDocument()
  })

  it('still requires sign-in before checking roles', () => {
    renderWithAuth(routes, '/student', fakeAuth({ status: 'anonymous', user: null }))
    expect(screen.getByRole('heading', { name: 'Login page' })).toBeInTheDocument()
  })
})
