import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'

/** A fake session so tests can set any auth state without a backend. */
export function fakeAuth(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    status: 'authenticated',
    user: { id: '00000000-0000-0000-0000-000000000001', email: 'test.student@upr.edu' },
    roles: [],
    error: null,
    refresh: async () => {},
    logout: async () => {},
    ...overrides,
  }
}

export function renderWithAuth(routes: RouteObject[], path: string, auth = fakeAuth()) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <AuthContext.Provider value={auth}>
      <RouterProvider router={router} />
    </AuthContext.Provider>,
  )
  return router
}
