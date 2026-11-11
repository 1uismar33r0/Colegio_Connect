import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { routes } from '../../router'
import {
  adminFixtures,
  apiFailure,
  expectNoAxeViolations,
  mockAdminApi,
  pending,
} from '../../test/mockApi'
import { fakeAuth, renderWithAuth } from '../../test/renderWithAuth'

vi.mock('../../api/admin')

const renderQueue = () => renderWithAuth(routes, '/moderation', fakeAuth({ roles: ['MODERATOR'] }))

const page = (n: number, totalPages: number) => ({
  ...adminFixtures.pendingCompanies,
  page: n,
  totalPages,
  totalElements: 25,
  items: [{ id: `0190bbbb-0000-7000-8000-00000000010${n}`, name: `Company page ${n + 1}` }],
})

beforeEach(() => {
  mockAdminApi()
})

describe('Administrator Review queue (GET /api/admin/organizations/pending)', () => {
  it('shows pending companies with summary tiles', async () => {
    const api = mockAdminApi()
    renderQueue()
    expect(screen.getByRole('heading', { level: 1, name: 'Review queue' })).toBeInTheDocument()

    const tiles = await screen.findByRole('list', { name: 'Queue summary' })
    expect(tiles).toHaveTextContent('Awaiting review02')
    expect(tiles).toHaveTextContent('Pending verification02')
    expect(within(tiles).getByLabelText('Not available yet')).toBeInTheDocument() // reports, #37
    expect(api.getPendingCompanies).toHaveBeenCalledWith(0)

    const table = screen.getByRole('table', { name: 'Items requiring review' })
    const headers = within(table).getAllByRole('columnheader')
    expect(headers.map((h) => h.textContent)).toEqual([
      'Organization',
      'Reason',
      'Status',
      'Actions',
    ])
    headers.forEach((h) => expect(h).toHaveAttribute('scope', 'col'))
    const rows = within(table).getAllByRole('row').slice(1)
    expect(rows).toHaveLength(adminFixtures.pendingCompanies.items.length)
    expect(rows[0]).toHaveTextContent('Green Block Network')
    expect(rows[0]).toHaveTextContent('New organization')
    expect(rows[0]).toHaveTextContent('Pending')
  })

  it('keeps Review disabled until the approval screens exist', async () => {
    renderQueue()
    const review = await screen.findByRole('button', { name: 'Review Green Block Network' })
    expect(review).toBeDisabled()
    expect(review).toHaveAccessibleDescription(/company approval screens \(#16\)/)
  })

  it('pages through the queue', async () => {
    const api = mockAdminApi({ getPendingCompanies: async (n = 0) => page(n, 2) })
    renderQueue()
    expect(await screen.findByText('Company page 1')).toBeInTheDocument()
    const pager = screen.getByRole('navigation', { name: 'Review queue pages' })
    expect(within(pager).getByRole('button', { name: 'Previous page' })).toBeDisabled()

    await userEvent.click(within(pager).getByRole('button', { name: 'Next page' }))
    expect(await screen.findByText('Company page 2')).toBeInTheDocument()
    expect(api.getPendingCompanies).toHaveBeenLastCalledWith(1)
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  })

  it('lists the administration sections in the sidebar', () => {
    renderQueue()
    const nav = screen.getByRole('navigation', { name: 'Administration sections' })
    expect(
      within(nav)
        .getAllByRole('link')
        .map((l) => l.textContent),
    ).toEqual(['Review queue', 'Organizations', 'Accounts', 'Reports', 'Audit log'])
  })

  it('shows the loading state', () => {
    mockAdminApi({ getPendingCompanies: pending })
    renderQueue()
    expect(screen.getByText('Loading the review queue…')).toBeInTheDocument()
  })

  it('shows an empty state when nothing is waiting', async () => {
    mockAdminApi({
      getPendingCompanies: async () => ({
        ...adminFixtures.pendingCompanies,
        items: [],
        totalElements: 0,
        totalPages: 0,
      }),
    })
    renderQueue()
    expect(await screen.findByText('Nothing is waiting for review.')).toBeInTheDocument()
  })

  it('explains a 403 (moderator role revoked mid-session) without a retry', async () => {
    mockAdminApi({ getPendingCompanies: () => apiFailure(403) })
    renderQueue()
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent("You don't have access to this information")
    expect(within(alert).queryByRole('button')).not.toBeInTheDocument()
  })

  it('retries after a server error', async () => {
    const api = mockAdminApi({ getPendingCompanies: () => apiFailure(500) })
    renderQueue()
    const alert = await screen.findByRole('alert')
    api.getPendingCompanies.mockImplementation(async () => adminFixtures.pendingCompanies)
    await userEvent.click(within(alert).getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('table', { name: 'Items requiring review' })).toBeInTheDocument()
  })

  it('has no axe violations', async () => {
    renderQueue()
    await screen.findByRole('table')
    await expectNoAxeViolations()
  })
})
