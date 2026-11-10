import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { routes } from '../../router'
import {
  ADMIN_IDS,
  adminFixtures,
  apiFailure,
  expectNoAxeViolations,
  mockAdminApi,
} from '../../test/mockApi'
import { fakeAuth, renderWithAuth } from '../../test/renderWithAuth'

vi.mock('../../api/admin')

const renderAudit = () =>
  renderWithAuth(routes, '/moderation/audit', fakeAuth({ roles: ['MODERATOR'] }))

beforeEach(() => {
  mockAdminApi()
})

describe('Audit log (GET /api/admin/audit-events)', () => {
  it('lists events newest first with readable actions and targets', async () => {
    const api = mockAdminApi()
    renderAudit()
    expect(screen.getByRole('heading', { level: 1, name: 'Audit log' })).toBeInTheDocument()

    const table = await screen.findByRole('table', { name: 'Recent events' })
    within(table)
      .getAllByRole('columnheader')
      .forEach((h) => expect(h).toHaveAttribute('scope', 'col'))
    const rows = within(table).getAllByRole('row').slice(1)
    expect(rows).toHaveLength(adminFixtures.auditEvents.items.length)
    expect(rows[0]).toHaveTextContent('Role assigned')
    expect(rows[0]).toHaveTextContent(`Account ${ADMIN_IDS.member}`)
    expect(rows[0]).toHaveTextContent(ADMIN_IDS.admin)
    expect(within(rows[0]).getByText(/Nov 10, 2026/)).toHaveAttribute(
      'dateTime',
      '2026-11-10T16:00:00Z',
    )
    expect(rows[1]).toHaveTextContent('Organization approved')
    expect(api.getAuditEvents).toHaveBeenCalledWith(0)
  })

  it('links account targets to role management, and only account targets', async () => {
    renderAudit()
    const table = await screen.findByRole('table', { name: 'Recent events' })
    const links = within(table).getAllByRole('link', { name: /Manage roles/ })
    expect(links).toHaveLength(1)
    expect(links[0]).toHaveAccessibleName(`Manage roles for ${ADMIN_IDS.member}`)

    await userEvent.click(links[0])
    expect(await screen.findByRole('heading', { level: 1, name: 'Accounts' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^User ID/)).toHaveValue(ADMIN_IDS.member)
    expect(await screen.findByRole('table', { name: 'Current roles' })).toBeInTheDocument()
  })

  it('shows an empty state', async () => {
    mockAdminApi({
      getAuditEvents: async () => ({ ...adminFixtures.auditEvents, items: [], totalPages: 0 }),
    })
    renderAudit()
    expect(await screen.findByText('No audited actions yet.')).toBeInTheDocument()
  })

  it('retries after a server error', async () => {
    const api = mockAdminApi({ getAuditEvents: () => apiFailure(500) })
    renderAudit()
    const alert = await screen.findByRole('alert')
    api.getAuditEvents.mockImplementation(async () => adminFixtures.auditEvents)
    await userEvent.click(within(alert).getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('table', { name: 'Recent events' })).toBeInTheDocument()
  })

  it('has no axe violations', async () => {
    renderAudit()
    await screen.findByRole('table')
    await expectNoAxeViolations()
  })
})
