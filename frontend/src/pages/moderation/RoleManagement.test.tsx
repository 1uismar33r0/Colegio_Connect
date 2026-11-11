import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/problem'
import { routes } from '../../router'
import { ADMIN_IDS, adminFixtures, expectNoAxeViolations, mockAdminApi } from '../../test/mockApi'
import { fakeAuth, renderWithAuth } from '../../test/renderWithAuth'

vi.mock('../../api/admin')

const renderAccounts = (query = '') =>
  renderWithAuth(routes, `/moderation/accounts${query}`, fakeAuth({ roles: ['MODERATOR'] }))

const rejectWith = (status: number, detail: string) =>
  Promise.reject(new ApiError(status, { status, detail }))

beforeEach(() => {
  mockAdminApi()
})

describe('Accounts: role management (/api/admin/role-assignments)', () => {
  it('validates the user ID before calling the API', async () => {
    const api = mockAdminApi()
    renderAccounts()
    await userEvent.type(screen.getByLabelText(/^User ID/), 'not-a-uuid')
    await userEvent.click(screen.getByRole('button', { name: 'Show roles' }))

    const input = screen.getByLabelText(/^User ID/)
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveFocus()
    expect(input).toHaveAccessibleDescription(/Enter the full user ID/)
    expect(api.getRoleAssignments).not.toHaveBeenCalled()
  })

  it('looks up an account and lists its roles', async () => {
    const api = mockAdminApi()
    const router = renderAccounts()
    await userEvent.type(screen.getByLabelText(/^User ID/), ADMIN_IDS.member.toUpperCase())
    await userEvent.click(screen.getByRole('button', { name: 'Show roles' }))

    expect(router.state.location.search).toBe(`?userId=${ADMIN_IDS.member}`)
    const table = await screen.findByRole('table', { name: 'Current roles' })
    expect(api.getRoleAssignments).toHaveBeenCalledWith(ADMIN_IDS.member)
    const rows = within(table).getAllByRole('row').slice(1)
    expect(rows[0]).toHaveTextContent('Student')
    expect(rows[0]).toHaveTextContent('Platform-wide')
    expect(rows[0]).toHaveTextContent('Not managed here') // STUDENT is not revocable here
    expect(rows[1]).toHaveTextContent('University office or department')
    expect(rows[1]).toHaveTextContent(ADMIN_IDS.office)
  })

  it('shows an empty state for an account without roles', async () => {
    mockAdminApi({ getRoleAssignments: async () => [] })
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    expect(await screen.findByText('This account has no roles yet.')).toBeInTheDocument()
  })

  it('revokes a role after confirmation and reloads the list', async () => {
    const api = mockAdminApi()
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    await userEvent.click(
      await screen.findByRole('button', { name: 'Revoke University office or department' }),
    )

    const confirm = screen.getByRole('group', { name: /Revoke University office/ })
    expect(within(confirm).getByRole('button', { name: 'Confirm revoke' })).toHaveFocus()
    expect(api.revokeRole).not.toHaveBeenCalled()

    api.getRoleAssignments.mockImplementation(async () => [adminFixtures.memberRoles[0]])
    await userEvent.click(within(confirm).getByRole('button', { name: 'Confirm revoke' }))

    expect(api.revokeRole).toHaveBeenCalledWith(adminFixtures.memberRoles[1].id)
    expect(await screen.findByRole('status')).toHaveTextContent('Role revoked.')
    expect(
      screen.queryByRole('button', { name: 'Revoke University office or department' }),
    ).not.toBeInTheDocument()
  })

  it('cancels a revoke without calling the API', async () => {
    const api = mockAdminApi()
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    await userEvent.click(
      await screen.findByRole('button', { name: 'Revoke University office or department' }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('group', { name: /Revoke/ })).not.toBeInTheDocument()
    expect(api.revokeRole).not.toHaveBeenCalled()
  })

  it("shows the backend's reason when a revoke is refused", async () => {
    mockAdminApi({
      getRoleAssignments: async () => [
        { id: '0190dddd-0000-7000-8000-000000000009', userId: ADMIN_IDS.admin, role: 'MODERATOR' },
      ],
      revokeRole: () => rejectWith(400, 'You cannot remove your own moderator role.'),
    })
    renderAccounts(`?userId=${ADMIN_IDS.admin}`)
    await userEvent.click(
      await screen.findByRole('button', { name: 'Revoke Administrator / moderator' }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Confirm revoke' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'You cannot remove your own moderator role.',
    )
  })

  it('assigns an institutional role with an organization', async () => {
    const api = mockAdminApi({ getRoleAssignments: async () => [] })
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    const form = await screen.findByRole('form', { name: 'Assign a role' })

    await userEvent.type(within(form).getByLabelText(/^Organization ID/), ADMIN_IDS.office)
    await userEvent.click(within(form).getByRole('button', { name: 'Assign role' }))

    expect(api.assignRole).toHaveBeenCalledWith({
      userId: ADMIN_IDS.member,
      role: 'INSTITUTIONAL',
      organizationId: ADMIN_IDS.office,
    })
    expect(await screen.findByRole('status')).toHaveTextContent('Role assigned.')
    expect(api.getRoleAssignments).toHaveBeenCalledTimes(2) // reloaded after the change
  })

  it('assigns the moderator role without an organization', async () => {
    const api = mockAdminApi({ getRoleAssignments: async () => [] })
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    const form = await screen.findByRole('form', { name: 'Assign a role' })

    await userEvent.selectOptions(within(form).getByLabelText('Role'), 'MODERATOR')
    expect(within(form).queryByLabelText(/^Organization ID/)).not.toBeInTheDocument()
    await userEvent.click(within(form).getByRole('button', { name: 'Assign role' }))

    expect(api.assignRole).toHaveBeenCalledWith({
      userId: ADMIN_IDS.member,
      role: 'MODERATOR',
      organizationId: undefined,
    })
  })

  it('requires an organization for the institutional role', async () => {
    const api = mockAdminApi({ getRoleAssignments: async () => [] })
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    const form = await screen.findByRole('form', { name: 'Assign a role' })
    await userEvent.click(within(form).getByRole('button', { name: 'Assign role' }))

    const org = within(form).getByLabelText(/^Organization ID/)
    expect(org).toHaveAttribute('aria-invalid', 'true')
    expect(org).toHaveFocus()
    expect(api.assignRole).not.toHaveBeenCalled()
  })

  it.each([
    [409, 'The user already has this role.'],
    [400, 'Only verified UPRM members can receive this role.'],
    [404, 'Organization not found.'],
  ])("shows the backend's %i message on assign", async (status, detail) => {
    mockAdminApi({
      getRoleAssignments: async () => [],
      assignRole: () => rejectWith(status, detail),
    })
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    const form = await screen.findByRole('form', { name: 'Assign a role' })
    await userEvent.type(within(form).getByLabelText(/^Organization ID/), ADMIN_IDS.office)
    await userEvent.click(within(form).getByRole('button', { name: 'Assign role' }))
    expect(await within(form).findByRole('alert')).toHaveTextContent(detail)
  })

  it('keeps server errors generic', async () => {
    mockAdminApi({
      getRoleAssignments: async () => [],
      assignRole: () => rejectWith(500, 'NullPointerException at RoleAssignmentService'),
    })
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    const form = await screen.findByRole('form', { name: 'Assign a role' })
    await userEvent.selectOptions(within(form).getByLabelText('Role'), 'MODERATOR')
    await userEvent.click(within(form).getByRole('button', { name: 'Assign role' }))
    const alert = await within(form).findByRole('alert')
    expect(alert).toHaveTextContent('Something went wrong on our side')
    expect(alert).not.toHaveTextContent('NullPointerException')
  })

  it('has no axe violations with roles listed and a confirmation open', async () => {
    renderAccounts(`?userId=${ADMIN_IDS.member}`)
    await userEvent.click(
      await screen.findByRole('button', { name: 'Revoke University office or department' }),
    )
    await expectNoAxeViolations()
  })
})
