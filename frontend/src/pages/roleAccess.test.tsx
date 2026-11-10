import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Role } from '../auth/roles'
import { routes } from '../router'
import { expectNoAxeViolations, mockRoleApi } from '../test/mockApi'
import { fakeAuth, renderWithAuth } from '../test/renderWithAuth'

vi.mock('../api/roleData')

const renderAs = (path: string, roles: Role[]) => renderWithAuth(routes, path, fakeAuth({ roles }))

const HOMES: [string, Role, string][] = [
  ['/student', 'STUDENT', 'Find your next opportunity'],
  ['/institution', 'INSTITUTIONAL', 'Office overview'],
  ['/company', 'COMPANY', 'Organization dashboard'],
  ['/moderation', 'MODERATOR', 'Review queue'],
]

beforeEach(() => {
  mockRoleApi()
})

describe('role areas', () => {
  it.each(HOMES)('%s opens for %s', async (path, role, heading) => {
    renderAs(path, [role])
    expect(await screen.findByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
  })

  it.each(HOMES.filter(([, role]) => role !== 'MODERATOR'))(
    '%s sends users without %s to /forbidden',
    (path, role) => {
      const other: Role = role === 'STUDENT' ? 'COMPANY' : 'STUDENT'
      const router = renderAs(path, [other])
      expect(router.state.location.pathname).toBe('/forbidden')
    },
  )

  it('keeps non-admins out of moderation', () => {
    const router = renderAs('/moderation', ['STUDENT', 'INSTITUTIONAL', 'COMPANY'])
    expect(router.state.location.pathname).toBe('/forbidden')
  })

  it.each(HOMES)(
    'the administrator can open %s (all-access decision)',
    async (path, _r, heading) => {
      renderAs(path, ['MODERATOR'])
      expect(await screen.findByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
    },
  )

  it('sends anonymous visitors to sign in', () => {
    const router = renderWithAuth(routes, '/student', fakeAuth({ status: 'anonymous', user: null }))
    expect(router.state.location.pathname).toBe('/login')
  })

  it('renders a placeholder that names the issue for each unbuilt section', async () => {
    renderAs('/moderation/accounts', ['MODERATOR'])
    expect(await screen.findByRole('heading', { level: 1, name: 'Accounts' })).toBeInTheDocument()
    expect(screen.getByText(/built in issue #39/)).toBeInTheDocument()
    const nav = screen.getByRole('navigation', { name: 'Administration sections' })
    expect(within(nav).getByRole('link', { name: 'Accounts' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(document.title).toBe('Accounts · Colegio Connect')
  })

  it('shows unknown sub-paths as not found', () => {
    renderAs('/student/nope', ['STUDENT'])
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/not found/i)
  })
})

describe('Dashboard', () => {
  it('tells a new account that an administrator still has to assign a role', async () => {
    renderAs('/dashboard', [])
    expect(screen.getByRole('status')).toHaveTextContent(
      'An administrator still needs to assign your role',
    )
    await expectNoAxeViolations()
  })

  it('links to the areas the user can open', () => {
    renderAs('/dashboard', ['STUDENT'])
    const areas = screen.getByRole('region', { name: 'Your areas' })
    expect(
      within(areas)
        .getAllByRole('link')
        .map((l) => l.textContent),
    ).toEqual(['Student'])
  })

  it('links the administrator to every area', () => {
    renderAs('/dashboard', ['MODERATOR'])
    const areas = screen.getByRole('region', { name: 'Your areas' })
    expect(within(areas).getAllByRole('link')).toHaveLength(4)
  })
})
