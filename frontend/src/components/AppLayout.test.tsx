import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import axe from 'axe-core'
import { Link } from 'react-router'
import { describe, expect, it } from 'vitest'
import { fakeAuth, renderWithAuth } from '../test/renderWithAuth'
import { AppLayout } from './AppLayout'

const routes = [
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Link to="/about">About</Link>, handle: { title: 'Home' } },
      { path: 'about', element: <h1>About page</h1>, handle: { title: 'About' } },
    ],
  },
]

describe('AppLayout', () => {
  it('has a skip link that targets main', () => {
    renderWithAuth(routes, '/')
    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute(
      'href',
      '#main',
    )
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main')
  })

  it('moves focus to main and updates the title after navigation', async () => {
    renderWithAuth(routes, '/')
    expect(document.title).toBe('Home · Colegio Connect')
    expect(screen.getByRole('main')).not.toHaveFocus()

    await userEvent.click(screen.getByRole('link', { name: 'About' }))

    expect(await screen.findByRole('heading', { name: 'About page' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveFocus()
    expect(document.title).toBe('About · Colegio Connect')
  })

  it('shows only the role areas the user holds', () => {
    renderWithAuth(routes, '/', fakeAuth({ roles: ['STUDENT'] }))
    const nav = screen.getByRole('navigation', { name: 'Primary' })
    expect(nav).toHaveTextContent('Student')
    expect(nav).not.toHaveTextContent('Moderation')
    expect(nav).not.toHaveTextContent('Company')
  })

  it('shows every area to the administrator', () => {
    renderWithAuth(routes, '/', fakeAuth({ roles: ['MODERATOR'] }))
    const nav = screen.getByRole('navigation', { name: 'Primary' })
    for (const area of ['Student', 'Institution', 'Company', 'Moderation']) {
      expect(nav).toHaveTextContent(area)
    }
  })

  it('labels the account with the roles it holds', () => {
    renderWithAuth(routes, '/', fakeAuth({ roles: ['INSTITUTIONAL', 'MODERATOR'] }))
    expect(screen.getByText('University office, Administrator')).toBeInTheDocument()
  })

  it('has no axe violations', async () => {
    renderWithAuth(routes, '/')
    // jsdom can't render colors; contrast is verified in a real browser instead.
    const results = await axe.run(document.body, {
      rules: { 'color-contrast': { enabled: false } },
    })
    expect(results.violations).toEqual([])
  })
})
