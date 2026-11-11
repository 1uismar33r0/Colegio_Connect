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

const renderOverview = (path = '/institution') =>
  renderWithAuth(routes, path, fakeAuth({ roles: ['INSTITUTIONAL'] }))

const needsSelection = () => apiFailure(409, 'Select an organizationId for this dashboard.')

beforeEach(() => {
  mockRoleApi()
})

describe('University office Overview', () => {
  it('shows the summary tiles and recent opportunities', async () => {
    const api = mockRoleApi()
    renderOverview()
    expect(screen.getByRole('heading', { level: 1, name: 'Office overview' })).toBeInTheDocument()

    const tiles = await screen.findByRole('list', { name: 'Office summary' })
    expect(tiles).toHaveTextContent('Published opportunities08')
    expect(tiles).toHaveTextContent('New responses12')
    expect(tiles).toHaveTextContent('Drafts02')

    const table = screen.getByRole('table', { name: 'Recent opportunities' })
    const headers = within(table).getAllByRole('columnheader')
    expect(headers.map((h) => h.textContent)).toEqual([
      'Opportunity',
      'Type',
      'Responses',
      'Status',
      'Actions',
    ])
    headers.forEach((h) => expect(h).toHaveAttribute('scope', 'col'))
    expect(within(table).getAllByRole('row')).toHaveLength(
      fixtures.officeOverview.recent.length + 1,
    )
    // With one eligible office the backend chooses it; the page sends no organization.
    expect(api.getOfficeOverview).toHaveBeenCalledWith(undefined)
  })

  it('shows Edit and no response count for drafts', async () => {
    renderOverview()
    const draft = (await screen.findByText('Fall Research Participation Study')).closest('tr')!
    expect(draft).toHaveTextContent('—')
    expect(draft).toHaveTextContent('Draft')
    expect(
      within(draft).getByRole('button', { name: 'Edit Fall Research Participation Study' }),
    ).toBeDisabled()
    const published = screen.getByText('Student Research Assistant').closest('tr')!
    expect(published).toHaveTextContent('3 new')
    expect(within(published).getByRole('button', { name: /^Manage/ })).toBeInTheDocument()
  })

  it('keeps the primary action at the top and explains why it is unavailable', () => {
    renderOverview()
    const create = screen.getByRole('button', { name: '+ Create opportunity' })
    expect(create).toBeDisabled()
    expect(create).toHaveAccessibleDescription(/publisher screens \(#20\)/)
  })

  it('shows the loading state', () => {
    mockRoleApi({ getOfficeOverview: pending })
    renderOverview()
    expect(screen.getByText('Loading your office overview…')).toBeInTheDocument()
  })

  it('shows an empty state with no opportunities', async () => {
    mockRoleApi({
      getOfficeOverview: async () => ({ ...fixtures.officeOverview, recent: [] }),
    })
    renderOverview()
    expect(
      await screen.findByText("You haven't created any opportunities yet."),
    ).toBeInTheDocument()
  })

  it('retries after an error', async () => {
    const api = mockRoleApi({ getOfficeOverview: () => apiFailure(503) })
    renderOverview()
    const alert = await screen.findByRole('alert')
    api.getOfficeOverview.mockImplementation(async () => fixtures.officeOverview)
    await userEvent.click(within(alert).getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('table', { name: 'Recent opportunities' })).toBeInTheDocument()
  })

  it('explains a 403 for an office the user does not belong to', async () => {
    mockRoleApi({ getOfficeOverview: () => apiFailure(403) })
    renderOverview()
    expect(await screen.findByRole('alert')).toHaveTextContent(
      "You don't have access to this information",
    )
  })

  describe('with more than one office', () => {
    it('asks which office to open instead of picking one', async () => {
      const api = mockRoleApi({
        getOfficeOverview: async (organizationId) =>
          organizationId ? fixtures.officeOverview : needsSelection(),
      })
      const router = renderOverview()

      const picker = await screen.findByRole('region', { name: 'Choose an organization' })
      expect(within(picker).getByRole('status')).toHaveTextContent(
        'Your account is linked to more than one office or department.',
      )
      expect(screen.queryByRole('table')).not.toBeInTheDocument()

      await userEvent.type(within(picker).getByLabelText('Organization ID'), ADMIN_IDS.office)
      await userEvent.click(within(picker).getByRole('button', { name: 'Open dashboard' }))

      expect(await screen.findByRole('table', { name: 'Recent opportunities' })).toBeInTheDocument()
      expect(api.getOfficeOverview).toHaveBeenLastCalledWith(ADMIN_IDS.office)
      expect(router.state.location.search).toBe(`?organizationId=${ADMIN_IDS.office}`)
      expect(screen.getByText(ADMIN_IDS.office)).toBeInTheDocument()
    })

    it('validates the organization ID and keeps focus on the field', async () => {
      const api = mockRoleApi({ getOfficeOverview: needsSelection })
      renderOverview()
      const field = await screen.findByLabelText('Organization ID')
      const calls = api.getOfficeOverview.mock.calls.length

      await userEvent.type(field, 'not-an-id')
      await userEvent.click(screen.getByRole('button', { name: 'Open dashboard' }))

      expect(field).toHaveFocus()
      expect(field).toHaveAccessibleDescription(/Enter the full organization ID/)
      expect(api.getOfficeOverview).toHaveBeenCalledTimes(calls)
    })

    it('opens the office named in the address and lets the user switch', async () => {
      const api = mockRoleApi({
        getOfficeOverview: async (organizationId) =>
          organizationId ? fixtures.officeOverview : needsSelection(),
      })
      renderOverview(`/institution?organizationId=${ADMIN_IDS.office}`)

      expect(await screen.findByRole('table')).toBeInTheDocument()
      expect(api.getOfficeOverview).toHaveBeenCalledWith(ADMIN_IDS.office)

      await userEvent.click(screen.getByRole('link', { name: 'Choose another organization' }))
      expect(
        await screen.findByRole('region', { name: 'Choose an organization' }),
      ).toBeInTheDocument()
    })

    it('rejects a malformed ID in the address without calling the API', async () => {
      const api = mockRoleApi()
      renderOverview('/institution?organizationId=123')
      expect(await screen.findByText(/That organization ID isn't valid/)).toBeInTheDocument()
      expect(api.getOfficeOverview).not.toHaveBeenCalled()
    })

    it('has no axe violations in the selection state', async () => {
      mockRoleApi({ getOfficeOverview: needsSelection })
      renderOverview()
      await screen.findByRole('region', { name: 'Choose an organization' })
      await expectNoAxeViolations()
    })
  })

  it('has no axe violations', async () => {
    renderOverview()
    await screen.findByRole('table')
    await expectNoAxeViolations()
  })
})
