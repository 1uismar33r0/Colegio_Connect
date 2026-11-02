import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it, vi } from 'vitest'
import App from './App'

it('shows the login form and reports invalid credentials from a Problem Detail response', async () => {
  const unauthorized = () => new Response(JSON.stringify({ status: 401, title: 'Unauthorized', detail: 'Invalid email or password.' }), { status: 401, headers: { 'Content-Type': 'application/problem+json' } })
  vi.stubGlobal('fetch', vi.fn()
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(unauthorized())
    .mockResolvedValueOnce(unauthorized()))

  const user = userEvent.setup()
  render(<App />)

  await user.click(await screen.findByRole('button', { name: 'Sign in' }))
  await user.type(screen.getByLabelText('Email'), 'student@upr.edu')
  await user.type(screen.getByLabelText('Password'), 'wrong-password')
  await user.click(screen.getByRole('button', { name: 'Sign in' }))

  expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
})
