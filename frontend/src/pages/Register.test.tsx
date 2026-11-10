import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { register } from '../api/auth'
import { apiError, axeViolations } from '../test/authFlow'
import { Landing, TestMain } from '../test/TestComponents'
import { fakeAuth, renderWithAuth } from '../test/renderWithAuth'
import { Register } from './Register'

vi.mock('../api/auth')

const routes = [
  {
    element: <TestMain />,
    children: [
      { path: '/register', element: <Register /> },
      { path: '/verify', element: <Landing name="Verify page" /> },
      { path: '/login', element: <Landing name="Login page" /> },
    ],
  },
]
const anonymous = fakeAuth({ status: 'anonymous', user: null })

function renderRegister() {
  renderWithAuth(routes, '/register', anonymous)
}

async function fillAndSubmit(email: string, password: string, confirm = password) {
  const user = userEvent.setup()
  if (email) await user.type(screen.getByLabelText(/^Email/), email)
  if (password) await user.type(screen.getByLabelText(/^Password/), password)
  if (confirm) await user.type(screen.getByLabelText(/^Confirm password/), confirm)
  await user.click(screen.getByRole('button', { name: 'Create account' }))
}

describe('Register', () => {
  beforeEach(() => {
    vi.mocked(register).mockReset()
  })

  it('registers and moves to verification with the email in router state', async () => {
    vi.mocked(register).mockResolvedValue(undefined)
    renderRegister()

    await fillAndSubmit(' New.Student@UPR.edu ', 'correct-horse')

    expect(await screen.findByRole('heading', { name: 'Verify page' })).toBeInTheDocument()
    expect(register).toHaveBeenCalledWith('new.student@upr.edu', 'correct-horse')
    expect(screen.getByTestId('state')).toHaveTextContent('"email":"new.student@upr.edu"')
  })

  it('validates on the client, announces the problem and focuses the first invalid field', async () => {
    renderRegister()

    await fillAndSubmit('someone@gmail.com', 'short', 'different')

    expect(register).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Fix the highlighted fields')
    const email = screen.getByLabelText(/^Email/)
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(email).toHaveAccessibleDescription(/UPRM institutional email/)
    expect(email).toHaveFocus()
    expect(screen.getByLabelText(/^Password/)).toHaveAccessibleDescription(/at least 8/)
    expect(screen.getByLabelText(/^Confirm password/)).toHaveAccessibleDescription(/do not match/)
  })

  it('offers sign-in and verification when the email is already registered', async () => {
    vi.mocked(register).mockRejectedValue(apiError(409, { detail: 'Email is already registered.' }))
    renderRegister()

    await fillAndSubmit('taken@upr.edu', 'correct-horse')

    expect(await screen.findByRole('alert')).toHaveTextContent('Email is already registered.')
    expect(screen.getByRole('link', { name: 'enter your verification code' })).toBeInTheDocument()
  })

  it('maps backend field errors onto the matching inputs', async () => {
    vi.mocked(register).mockRejectedValue(
      apiError(400, {
        detail: 'One or more fields are invalid.',
        errors: { password: 'size must be between 8 and 64' },
      }),
    )
    renderRegister()

    await fillAndSubmit('a@upr.edu', 'correct-horse')

    const password = await screen.findByLabelText(/^Password/)
    expect(password).toHaveAttribute('aria-invalid', 'true')
    expect(password).toHaveAccessibleDescription(/size must be between 8 and 64/)
  })

  it('hides server error details', async () => {
    vi.mocked(register).mockRejectedValue(apiError(500, { detail: 'NullPointerException at …' }))
    renderRegister()

    await fillAndSubmit('a@upr.edu', 'correct-horse')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Something went wrong on our side')
    expect(alert).not.toHaveTextContent('NullPointerException')
  })

  it('can be completed with the keyboard alone', async () => {
    vi.mocked(register).mockResolvedValue(undefined)
    renderRegister()
    const user = userEvent.setup()

    await user.tab()
    expect(screen.getByLabelText(/^Email/)).toHaveFocus()
    await user.keyboard('kb@upr.edu')
    await user.tab()
    await user.keyboard('correct-horse')
    await user.tab()
    await user.keyboard('correct-horse{Enter}')

    expect(await screen.findByRole('heading', { name: 'Verify page' })).toBeInTheDocument()
  })

  it('has no axe violations, including the error state', async () => {
    renderRegister()
    expect(await axeViolations()).toEqual([])

    await fillAndSubmit('', '')
    expect(await axeViolations()).toEqual([])
  })
})
