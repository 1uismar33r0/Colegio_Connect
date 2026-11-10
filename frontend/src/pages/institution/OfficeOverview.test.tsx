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

const renderOverview = () =>
  renderWithAuth(routes, '/institution', fakeAuth({ roles: ['INSTITUTIONAL'] }))

beforeEach(() => {
  mockRoleApi()
})

describe('University office Overview', () => {
  it('shows the summary tiles and recent opportunities', async () => {
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
  })

  it('shows Edit and no response count for drafts', async () => {
    renderOverview()
    const draft = (await screen.findByText('Fall Volunteer Orientation')).closest('tr')!
    expect(draft).toHaveTextContent('—')
    expect(draft).toHaveTextContent('Draft')
    expect(
      within(draft).getByRole('button', { name: 'Edit Fall Volunteer Orientation' }),
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

  it('has no axe violations', async () => {
    renderOverview()
    await screen.findByRole('table')
    await expectNoAxeViolations()
  })
})
