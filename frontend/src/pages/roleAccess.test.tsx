import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Role } from '../auth/roles'
import { routes } from '../router'
import { expectNoAxeViolations, mockAdminApi, mockRoleApi } from '../test/mockApi'
import { fakeAuth, renderWithAuth } from '../test/renderWithAuth'

vi.mock('../api/roleData')
vi.mock('../api/admin')

const renderAs = (path: string, roles: Role[]) => renderWithAuth(routes, path, fakeAuth({ roles }))

const HOMES: [string, Role, string][] = [
  ['/student', 'STUDENT', 'Find your next opportunity'],
  ['/institution', 'INSTITUTIONAL', 'Office overview'],
  ['/company', 'COMPANY', 'Organization dashboard'],
  ['/moderation', 'MODERATOR', 'Review queue'],
]

beforeEach(() => {
  mockRoleApi()
  mockAdminApi()
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

  it('lets an external-organization account without a role open its read-only dashboard', async () => {
    // Pending company owners hold no COMPANY role (PR #127); the backend decides what they see.
    const router = renderWithAuth(
      routes,
      '/company',
      fakeAuth({
        roles: [],
        user: { id: '1', email: 'owner@example.test', accountType: 'EXTERNAL_ORGANIZATION' },
      }),
    )
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Organization dashboard' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/company')
    expect(screen.getByRole('navigation', { name: 'Primary' })).toHaveTextContent('Company')
  })

  it('keeps a UPRM account without the role out of the company area', () => {
    const router = renderWithAuth(
      routes,
      '/company',
      fakeAuth({ roles: [], user: { id: '1', email: 'a@upr.edu', accountType: 'UPRM_MEMBER' } }),
    )
    expect(router.state.location.pathname).toBe('/forbidden')
  })

  it('sends anonymous visitors to sign in', () => {
    const router = renderWithAuth(routes, '/student', fakeAuth({ status: 'anonymous', user: null }))
    expect(router.state.location.pathname).toBe('/login')
  })

  it('renders a placeholder that names the issue for each unbuilt section', async () => {
    renderAs('/moderation/reports', ['MODERATOR'])
    expect(await screen.findByRole('heading', { level: 1, name: 'Reports' })).toBeInTheDocument()
    expect(screen.getByText(/built in issue #37/)).toBeInTheDocument()
    const nav = screen.getByRole('navigation', { name: 'Administration sections' })
    expect(within(nav).getByRole('link', { name: 'Reports' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(document.title).toBe('Reports · Colegio Connect')
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
