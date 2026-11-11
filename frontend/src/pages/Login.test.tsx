import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { login } from '../api/auth'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import type { AuthFlowState } from '../auth/flowState'
import { apiError, axeViolations } from '../test/authFlow'
import { Landing, TestMain } from '../test/TestComponents'
import { fakeAuth } from '../test/renderWithAuth'
import { Login } from './Login'

vi.mock('../api/auth')

const anonymous = () =>
  fakeAuth({ status: 'anonymous', user: null, refresh: vi.fn(async () => {}) })

function renderLogin(state?: AuthFlowState, auth: AuthContextValue = anonymous()) {
  const router = createMemoryRouter(
    [
      {
        element: <TestMain />,
        children: [
          { path: '/login', element: <Login /> },
          { path: '/dashboard', element: <Landing name="Dashboard page" /> },
          { path: '/student', element: <Landing name="Student page" /> },
          { path: '/verify', element: <Landing name="Verify page" /> },
        ],
      },
    ],
    { initialEntries: [{ pathname: '/login', state }] },
  )
  render(
    <AuthContext.Provider value={auth}>
      <RouterProvider router={router} />
    </AuthContext.Provider>,
  )
  return auth
}

async function signIn(email: string, password: string) {
  const user = userEvent.setup()
  if (email) await user.type(screen.getByLabelText(/^Email/), email)
  if (password) await user.type(screen.getByLabelText(/^Password/), password)
  await user.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('Login', () => {
  beforeEach(() => {
    vi.mocked(login).mockReset()
  })

  it('signs in, refreshes the session and returns to the guarded page', async () => {
    vi.mocked(login).mockResolvedValue({ id: '1', email: 'a@upr.edu' })
    const auth = renderLogin({ from: '/student' })

    expect(screen.getByText('Sign in to continue to /student.')).toBeInTheDocument()
    await signIn('A@upr.edu', 'correct-horse')

    expect(await screen.findByRole('heading', { name: 'Student page' })).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith('a@upr.edu', 'correct-horse')
    expect(auth.refresh).toHaveBeenCalledOnce()
  })

  it.each(['//evil.example', '/\\evil.example'])(
    'goes to the dashboard instead of the external return path %s',
    async (from) => {
      vi.mocked(login).mockResolvedValue({ id: '1', email: 'a@upr.edu' })
      renderLogin({ from })

      await signIn('a@upr.edu', 'correct-horse')

      expect(await screen.findByRole('heading', { name: 'Dashboard page' })).toBeInTheDocument()
    },
  )

  it('shows the notice and email handed over from verification', () => {
    renderLogin({ email: 'a@upr.edu', notice: 'Your email is verified. Sign in to continue.' })
    expect(screen.getByRole('status')).toHaveTextContent('Your email is verified.')
    expect(screen.getByLabelText(/^Email/)).toHaveValue('a@upr.edu')
  })

  it.each([
    [401, 'Invalid email or password.', undefined],
    [403, 'This account is suspended.', 'ACCOUNT_SUSPENDED'],
    [429, 'Too many failed attempts. Try again later.', undefined],
  ])('shows the backend message for %i', async (status, detail, code) => {
    vi.mocked(login).mockRejectedValue(apiError(status, { detail, code }))
    const auth = renderLogin()

    await signIn('a@upr.edu', 'wrong-password')

    expect(await screen.findByRole('alert')).toHaveTextContent(detail)
    expect(auth.refresh).not.toHaveBeenCalled()
  })

  it('sends unverified accounts (code EMAIL_NOT_VERIFIED) to the verify screen', async () => {
    vi.mocked(login).mockRejectedValue(
      apiError(403, { detail: 'Verify your email before logging in.', code: 'EMAIL_NOT_VERIFIED' }),
    )
    renderLogin()

    await signIn('new@upr.edu', 'correct-horse')

    expect(await screen.findByRole('heading', { name: 'Verify page' })).toBeInTheDocument()
    expect(screen.getByTestId('state')).toHaveTextContent('"email":"new@upr.edu"')
  })

  it('relies on the code, not the message text, to detect unverified accounts', async () => {
    // A reworded message must not change behaviour, and a 403 without the code is not "unverified".
    vi.mocked(login).mockRejectedValue(apiError(403, { detail: 'Please verify something else.' }))
    renderLogin()

    await signIn('a@upr.edu', 'correct-horse')

    expect(await screen.findByRole('alert')).toHaveTextContent('Please verify something else.')
    expect(screen.queryByRole('heading', { name: 'Verify page' })).not.toBeInTheDocument()
  })

  it('requires both fields before calling the API', async () => {
    renderLogin()

    await signIn('', '')

    expect(login).not.toHaveBeenCalled()
    expect(screen.getByLabelText(/^Email/)).toHaveFocus()
    expect(screen.getByLabelText(/^Password/)).toHaveAccessibleDescription('Enter your password.')
  })

  it('redirects users who are already signed in', async () => {
    renderLogin({ from: '/student' }, fakeAuth())
    expect(await screen.findByRole('heading', { name: 'Student page' })).toBeInTheDocument()
  })

  it('has no axe violations, including the error state', async () => {
    renderLogin({ notice: 'Your email is verified. Sign in to continue.' })
    expect(await axeViolations()).toEqual([])

    await signIn('', '')
    expect(await axeViolations()).toEqual([])
  })
})
