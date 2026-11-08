import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { resend, verify } from '../api/auth'
import { apiError, axeViolations } from '../test/authFlow'
import { Landing, TestMain } from '../test/TestComponents'
import { fakeAuth } from '../test/renderWithAuth'
import { Verify } from './Verify'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AuthContext } from '../auth/AuthContext'
import type { AuthFlowState } from '../auth/flowState'

vi.mock('../api/auth')

function renderVerify(state?: AuthFlowState) {
  const router = createMemoryRouter(
    [
      {
        element: <TestMain />,
        children: [
          { path: '/verify', element: <Verify /> },
          { path: '/login', element: <Landing name="Login page" /> },
        ],
      },
    ],
    { initialEntries: [{ pathname: '/verify', state }] },
  )
  render(
    <AuthContext.Provider value={fakeAuth({ status: 'anonymous', user: null })}>
      <RouterProvider router={router} />
    </AuthContext.Provider>,
  )
}

const fromRegistration: AuthFlowState = {
  email: 'new.student@upr.edu',
  notice: 'We sent a 6-digit code to new.student@upr.edu. It expires in 10 minutes.',
}

describe('Verify', () => {
  beforeEach(() => {
    vi.mocked(verify).mockReset()
    vi.mocked(resend).mockReset()
  })

  it('prefills the email from registration and verifies a spaced code', async () => {
    vi.mocked(verify).mockResolvedValue(undefined)
    renderVerify(fromRegistration)
    const user = userEvent.setup()

    expect(screen.getByRole('status')).toHaveTextContent('We sent a 6-digit code')
    expect(screen.getByLabelText(/^Email/)).toHaveValue('new.student@upr.edu')
    await user.type(screen.getByLabelText(/^Verification code/), '123 456')
    await user.click(screen.getByRole('button', { name: 'Verify email' }))

    expect(await screen.findByRole('heading', { name: 'Login page' })).toBeInTheDocument()
    expect(verify).toHaveBeenCalledWith('new.student@upr.edu', '123456')
    expect(screen.getByTestId('state')).toHaveTextContent('Your email is verified')
  })

  it('rejects a malformed code without calling the API', async () => {
    renderVerify(fromRegistration)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^Verification code/), '12ab')
    await user.click(screen.getByRole('button', { name: 'Verify email' }))

    expect(verify).not.toHaveBeenCalled()
    const code = screen.getByLabelText(/^Verification code/)
    expect(code).toHaveAccessibleDescription(/The code is 6 digits/)
    expect(code).toHaveFocus()
  })

  it('explains an invalid or expired code and keeps resend available', async () => {
    vi.mocked(verify).mockRejectedValue(
      apiError(400, { detail: 'Invalid or expired code. Request a new one.' }),
    )
    renderVerify({ email: 'new.student@upr.edu' }) // no notice: no code was just sent
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^Verification code/), '000000')
    await user.click(screen.getByRole('button', { name: 'Verify email' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid or expired code')
    expect(screen.getByRole('button', { name: 'Resend code' })).not.toHaveAttribute(
      'aria-disabled',
      'true',
    )
  })

  it('resends a code, confirms neutrally, then cools down', async () => {
    vi.mocked(resend).mockResolvedValue(undefined)
    renderVerify({ email: 'new.student@upr.edu' })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Resend code' }))

    expect(resend).toHaveBeenCalledWith('new.student@upr.edu')
    expect(await screen.findByRole('status')).toHaveTextContent(
      'If new.student@upr.edu still needs verifying, we sent it a new code.',
    )
    const button = screen.getByRole('button', { name: /Resend code \(available in \d+s\)/ })
    expect(button).toHaveAttribute('aria-disabled', 'true')

    await user.click(button)
    expect(resend).toHaveBeenCalledTimes(1)
  })

  it('starts in cooldown right after registration sent a code', () => {
    renderVerify(fromRegistration)
    expect(screen.getByRole('button', { name: /available in 60s/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    )
  })

  it('shows the rate-limit message from the backend', async () => {
    vi.mocked(resend).mockRejectedValue(
      apiError(429, { detail: 'Too many codes requested. Try again later.' }),
    )
    renderVerify({ email: 'new.student@upr.edu' })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Resend code' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Too many codes requested. Try again later.',
    )
    expect(screen.getByRole('button', { name: /available in/ })).toHaveAttribute(
      'aria-disabled',
      'true',
    )
  })

  it('asks for the email before resending when it is missing', async () => {
    renderVerify()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Resend code' }))

    expect(resend).not.toHaveBeenCalled()
    expect(screen.getByLabelText(/^Email/)).toHaveFocus()
    expect(screen.getByRole('alert')).toHaveTextContent('Enter your email to get a new code.')
  })

  it('has no axe violations, including the error state', async () => {
    renderVerify(fromRegistration)
    expect(await axeViolations()).toEqual([])

    await userEvent.click(screen.getByRole('button', { name: 'Verify email' }))
    expect(await axeViolations()).toEqual([])
  })
})
