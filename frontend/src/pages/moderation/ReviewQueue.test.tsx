import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { routes } from '../../router'
import {
  apiFailure,
  expectNoAxeViolations,
  fixtures,
  mockRoleApi,
  pending,
} from '../../test/mockApi'
import { fakeAuth, renderWithAuth } from '../../test/renderWithAuth'

vi.mock('../../api/roleData')

const renderQueue = () => renderWithAuth(routes, '/moderation', fakeAuth({ roles: ['MODERATOR'] }))

beforeEach(() => {
  mockRoleApi()
})

describe('Administrator Review queue', () => {
  it('shows the summary tiles and the items requiring review', async () => {
    renderQueue()
    expect(screen.getByRole('heading', { level: 1, name: 'Review queue' })).toBeInTheDocument()

    const tiles = await screen.findByRole('list', { name: 'Queue summary' })
    expect(tiles).toHaveTextContent('Awaiting review07')
    expect(tiles).toHaveTextContent('Reported listings02')
    expect(tiles).toHaveTextContent('Pending verification03')

    const table = screen.getByRole('table', { name: 'Items requiring review' })
    const headers = within(table).getAllByRole('columnheader')
    expect(headers.map((h) => h.textContent)).toEqual([
      'Submission',
      'Submitted by',
      'Reason',
      'Received',
      'Actions',
    ])
    headers.forEach((h) => expect(h).toHaveAttribute('scope', 'col'))

    const rows = within(table).getAllByRole('row').slice(1)
    expect(rows).toHaveLength(fixtures.reviewQueue.items.length)
    expect(rows[0]).toHaveTextContent('Green Block Network')
    expect(rows[0]).toHaveTextContent('New organization')
    expect(rows[1]).toHaveTextContent('Reported listing')
    expect(rows[2]).toHaveTextContent('Profile verification')
    expect(rows[2]).toHaveTextContent('Oct 21')
  })

  it('names each row action and keeps it disabled until moderation tools exist', async () => {
    renderQueue()
    const review = await screen.findByRole('button', {
      name: 'Review Student Innovation Challenge',
    })
    expect(review).toBeDisabled()
    expect(review).toHaveAccessibleDescription(/moderation tools \(#16, #39\)/)
    expect(
      screen.getByRole('button', { name: 'More actions for Student Innovation Challenge' }),
    ).toBeDisabled()
  })

  it('lists the administration sections in the sidebar', () => {
    renderQueue()
    const nav = screen.getByRole('navigation', { name: 'Administration sections' })
    expect(
      within(nav)
        .getAllByRole('link')
        .map((l) => l.textContent),
    ).toEqual(['Review queue', 'Organizations', 'Accounts', 'Reports'])
  })

  it('shows the loading state', () => {
    mockRoleApi({ getReviewQueue: pending })
    renderQueue()
    expect(screen.getByText('Loading the review queue…')).toBeInTheDocument()
  })

  it('shows an empty state when nothing is waiting', async () => {
    mockRoleApi({ getReviewQueue: async () => ({ ...fixtures.reviewQueue, items: [] }) })
    renderQueue()
    expect(await screen.findByText('Nothing is waiting for review.')).toBeInTheDocument()
  })

  it('retries after an error', async () => {
    const api = mockRoleApi({ getReviewQueue: () => apiFailure(500) })
    renderQueue()
    const alert = await screen.findByRole('alert')
    api.getReviewQueue.mockImplementation(async () => fixtures.reviewQueue)
    await userEvent.click(within(alert).getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('table', { name: 'Items requiring review' })).toBeInTheDocument()
  })

  it('has no axe violations', async () => {
    renderQueue()
    await screen.findByRole('table')
    await expectNoAxeViolations()
  })
})
