import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { routes } from '../../router'
import {
  ADMIN_IDS,
  apiFailure,
  expectNoAxeViolations,
  fixtures,
  mockRoleApi,
  pending,
} from '../../test/mockApi'
import { fakeAuth, renderWithAuth } from '../../test/renderWithAuth'

vi.mock('../../api/roleData')

const renderDashboard = () => renderWithAuth(routes, '/company', fakeAuth({ roles: ['COMPANY'] }))

const withApproval = (approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED') => async () => ({
  ...fixtures.companyDashboard,
  stats: { ...fixtures.companyDashboard.stats, approvalStatus },
})

beforeEach(() => {
  mockRoleApi()
})

describe('External organization Dashboard', () => {
  it('shows the summary tiles, active opportunities and needs-attention items', async () => {
    renderDashboard()
    expect(
      screen.getByRole('heading', { level: 1, name: 'Organization dashboard' }),
    ).toBeInTheDocument()

    const tiles = await screen.findByRole('list', { name: 'Organization summary' })
    expect(tiles).toHaveTextContent('Active listings04')
    expect(tiles).toHaveTextContent('Student responses17')
    expect(tiles).toHaveTextContent('Approval statusApproved')

    const active = screen.getByRole('region', { name: 'Active opportunities' })
    expect(within(active).getAllByRole('listitem')).toHaveLength(
      fixtures.companyDashboard.active.length,
    )
    expect(active).toHaveTextContent('Internship · Closes Jun 14')
    expect(active).toHaveTextContent('9 responses')
    expect(active).toHaveTextContent('1 response') // singular

    const attention = screen.getByRole('region', { name: 'Needs attention' })
    expect(attention).toHaveTextContent('New student responses')
    expect(attention).toHaveTextContent('4 new')
    expect(attention).toHaveTextContent('Organization approvalReviewed by')
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('shows a pending owner the read-only dashboard with the persisted approval state', async () => {
    mockRoleApi({ getCompanyDashboard: withApproval('PENDING') })
    renderDashboard()
    const tiles = await screen.findByRole('list', { name: 'Organization summary' })
    expect(tiles).toHaveTextContent('Approval statusPending approval')
    expect(screen.getByRole('status')).toHaveTextContent(
      "can't post opportunities until it's approved",
    )
    expect(screen.getByRole('button', { name: '+ Post an opportunity' })).toBeDisabled()
  })

  it('explains a rejected registration', async () => {
    mockRoleApi({ getCompanyDashboard: withApproval('REJECTED') })
    renderDashboard()
    const tiles = await screen.findByRole('list', { name: 'Organization summary' })
    expect(screen.getByRole('status')).toHaveTextContent('was not approved')
    expect(tiles).toHaveTextContent('Not approved')
  })

  it('disables posting until the publisher screens exist', () => {
    renderDashboard()
    const post = screen.getByRole('button', { name: '+ Post an opportunity' })
    expect(post).toBeDisabled()
    expect(post).toHaveAccessibleDescription(/publisher screens \(#20\)/)
  })

  it('shows the loading state', () => {
    mockRoleApi({ getCompanyDashboard: pending })
    renderDashboard()
    expect(screen.getByText('Loading your organization dashboard…')).toBeInTheDocument()
  })

  it('shows an empty state with no active opportunities', async () => {
    mockRoleApi({
      getCompanyDashboard: async () => ({ ...fixtures.companyDashboard, active: [] }),
    })
    renderDashboard()
    expect(await screen.findByText('You have no active opportunities.')).toBeInTheDocument()
  })

  it('retries after an error', async () => {
    const api = mockRoleApi({ getCompanyDashboard: () => apiFailure(500) })
    renderDashboard()
    const alert = await screen.findByRole('alert')
    api.getCompanyDashboard.mockImplementation(async () => fixtures.companyDashboard)
    await userEvent.click(within(alert).getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('region', { name: 'Needs attention' })).toBeInTheDocument()
  })

  it('asks which organization to open when the account has several', async () => {
    const api = mockRoleApi({
      getCompanyDashboard: async (organizationId) =>
        organizationId
          ? fixtures.companyDashboard
          : apiFailure(409, 'Select an organizationId for this dashboard.'),
    })
    renderDashboard()

    const picker = await screen.findByRole('region', { name: 'Choose an organization' })
    expect(picker).toHaveTextContent('linked to more than one organization')
    await userEvent.type(within(picker).getByLabelText('Organization ID'), ADMIN_IDS.pendingCompany)
    await userEvent.click(within(picker).getByRole('button', { name: 'Open dashboard' }))

    expect(await screen.findByRole('region', { name: 'Needs attention' })).toBeInTheDocument()
    expect(api.getCompanyDashboard).toHaveBeenLastCalledWith(ADMIN_IDS.pendingCompany)
  })

  it('has no axe violations', async () => {
    renderDashboard()
    await screen.findByRole('region', { name: 'Needs attention' })
    await expectNoAxeViolations()
  })

  it('has no axe violations for a pending owner', async () => {
    mockRoleApi({ getCompanyDashboard: withApproval('PENDING') })
    renderDashboard()
    await screen.findByText(/waiting for administrator approval/)
    await expectNoAxeViolations()
  })
})
