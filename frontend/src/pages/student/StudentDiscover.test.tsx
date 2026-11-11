import { fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FEED_PAGE_SIZE } from '../../api/listingOptions'
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

const renderDiscover = () => renderWithAuth(routes, '/student', fakeAuth({ roles: ['STUDENT'] }))

const applyFilters = () => userEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

beforeEach(() => {
  mockRoleApi()
})

describe('Student Discover', () => {
  it('shows opportunity cards and activity from the API', async () => {
    renderDiscover()

    expect(
      screen.getByRole('heading', { level: 1, name: 'Find your next opportunity' }),
    ).toBeInTheDocument()
    const card = await screen.findByRole('article', { name: 'Community Health Data Project' })
    expect(card).toHaveTextContent('University Office · Posted Nov 8')
    expect(card).toHaveTextContent('Capstone sponsorship')
    expect(within(card).getByRole('list', { name: 'Topics' })).toHaveTextContent('Data science')
    expect(screen.getAllByRole('article')).toHaveLength(fixtures.feed.length)

    const activity = screen.getByRole('region', { name: 'My activity' })
    expect(await within(activity).findByText('3 active')).toBeInTheDocument()
    expect(within(activity).getByText('5 saved')).toBeInTheDocument()
  })

  it('shows the verified publisher type apart from the publisher name', async () => {
    renderDiscover()
    const card = await screen.findByRole('article', { name: 'Summer Software Internship' })
    const label = within(card).getByText('Approved external company')
    expect(label).toHaveClass('trust-label')
    expect(label).not.toHaveTextContent('Northstar Labs')
    expect(within(card).getByText(/^Northstar Labs/)).toBeInTheDocument()
  })

  it('offers only the six approved categories and the three publisher types', async () => {
    renderDiscover()
    const options = (name: string) =>
      within(screen.getByLabelText(name))
        .getAllByRole('option')
        .map((o) => o.textContent)
    expect(options('Category')).toEqual([
      'All categories',
      'Job',
      'Internship',
      'Co-op',
      'Research position',
      'Research participation',
      'Capstone sponsorship',
    ])
    expect(options('Publisher')).toEqual([
      'All verified publishers',
      'Verified UPRM institution',
      'Approved external company',
      'Verified student project',
    ])
    expect(screen.queryByRole('option', { name: 'Event' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText(/field/i)).not.toBeInTheDocument()
  })

  it('marks Discover as the current section in the sidebar', () => {
    renderDiscover()
    const nav = screen.getByRole('navigation', { name: 'Student sections' })
    expect(within(nav).getByRole('link', { name: 'Discover' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(within(nav).getByRole('link', { name: 'Saved opportunities' })).not.toHaveAttribute(
      'aria-current',
    )
  })

  it('shows a loading status while the feed loads', () => {
    mockRoleApi({ getStudentFeed: pending, getStudentActivity: pending })
    renderDiscover()
    expect(screen.getByText('Loading opportunities…')).toBeInTheDocument()
  })

  it('sends the approved filters to the API and starts again at the first page', async () => {
    const api = mockRoleApi()
    renderDiscover()
    await screen.findAllByRole('article')

    api.getStudentFeed.mockImplementation(async () => [fixtures.feed[1]])
    await userEvent.type(screen.getByLabelText('Keyword'), '  software ')
    await userEvent.selectOptions(screen.getByLabelText('Category'), 'Internship')
    await userEvent.selectOptions(screen.getByLabelText('Publisher'), 'Approved external company')
    fireEvent.change(screen.getByLabelText('Closes on or before'), {
      target: { value: '2026-11-30' },
    })
    await applyFilters()

    expect(api.getStudentFeed).toHaveBeenLastCalledWith(
      {
        keyword: 'software',
        category: 'INTERNSHIP',
        sourceLabel: 'APPROVED_EXTERNAL_COMPANY',
        deadlineOnOrBefore: '2026-11-30',
        organizationId: undefined,
      },
      0,
    )
    expect(
      await screen.findByRole('article', { name: 'Summer Software Internship' }),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('article')).toHaveLength(1)
  })

  it('says the deadline is a Puerto Rico date', () => {
    renderDiscover()
    expect(screen.getByLabelText('Closes on or before')).toHaveAccessibleDescription(
      'Puerto Rico date',
    )
  })

  it('filters by organization ID and rejects a partial one before calling the API', async () => {
    const api = mockRoleApi()
    renderDiscover()
    await screen.findAllByRole('article')
    const calls = api.getStudentFeed.mock.calls.length

    await userEvent.click(screen.getByText('More filters'))
    const orgField = screen.getByLabelText('Organization ID')
    await userEvent.type(orgField, '0190bbbb')
    await applyFilters()

    expect(orgField).toHaveFocus()
    expect(orgField).toHaveAccessibleDescription(/Enter the full organization ID/)
    expect(api.getStudentFeed).toHaveBeenCalledTimes(calls)

    await userEvent.clear(orgField)
    await userEvent.type(orgField, ADMIN_IDS.office.toUpperCase())
    await applyFilters()
    expect(api.getStudentFeed).toHaveBeenLastCalledWith(
      expect.objectContaining({ organizationId: ADMIN_IDS.office }),
      0,
    )
    expect(orgField).not.toHaveAccessibleDescription()
  })

  it('explains an empty feed with and without filters', async () => {
    mockRoleApi({ getStudentFeed: async () => [] })
    renderDiscover()
    expect(
      await screen.findByText('There are no open opportunities right now.'),
    ).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Keyword'), 'nothing')
    await applyFilters()
    expect(await screen.findByText('No opportunities match these filters.')).toBeInTheDocument()
  })

  it('pages through a full feed', async () => {
    const fullPage = Array.from({ length: FEED_PAGE_SIZE }, (_, i) => ({
      ...fixtures.feed[0],
      id: `0190eeee-0000-7000-8000-${String(100 + i).padStart(12, '0')}`,
      title: `Opportunity ${i + 1}`,
    }))
    const api = mockRoleApi({ getStudentFeed: async () => fullPage })
    renderDiscover()
    await screen.findByRole('article', { name: 'Opportunity 1' })

    const pager = screen.getByRole('navigation', { name: 'Opportunity pages' })
    expect(within(pager).getByRole('button', { name: 'Previous page' })).toBeDisabled()
    api.getStudentFeed.mockImplementation(async () => [fixtures.feed[1]])
    await userEvent.click(within(pager).getByRole('button', { name: 'Next page' }))

    expect(api.getStudentFeed).toHaveBeenLastCalledWith({}, 1)
    expect(
      await screen.findByRole('article', { name: 'Summer Software Internship' }),
    ).toBeInTheDocument()
    const lastPager = screen.getByRole('navigation', { name: 'Opportunity pages' })
    expect(within(lastPager).getByRole('button', { name: 'Next page' })).toBeDisabled()
    expect(lastPager).toHaveTextContent('Page 2')
  })

  it('hides the pager when everything fits on one page', async () => {
    renderDiscover()
    await screen.findAllByRole('article')
    expect(screen.queryByRole('navigation', { name: 'Opportunity pages' })).not.toBeInTheDocument()
  })

  it('shows a retryable error and recovers on retry', async () => {
    const api = mockRoleApi({ getStudentFeed: () => apiFailure(500) })
    renderDiscover()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent("We couldn't load this information")
    expect(alert).not.toHaveTextContent('Synthetic test failure') // no technical detail

    api.getStudentFeed.mockImplementation(async () => fixtures.feed)
    await userEvent.click(within(alert).getByRole('button', { name: 'Try again' }))
    expect(
      await screen.findByRole('article', { name: 'Summer Software Internship' }),
    ).toBeInTheDocument()
  })

  it('explains a 403 without offering a retry', async () => {
    mockRoleApi({ getStudentFeed: () => apiFailure(403) })
    renderDiscover()
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent("You don't have access to this information")
    expect(within(alert).queryByRole('button')).not.toBeInTheDocument()
  })

  it('asks the user to sign in again on a 401', async () => {
    mockRoleApi({ getStudentFeed: () => apiFailure(401) })
    renderDiscover()
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Your session has ended')
    expect(within(alert).getByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login')
    expect(within(alert).queryByRole('button')).not.toBeInTheDocument()
  })

  it('disables actions that later issues build and says why', async () => {
    renderDiscover()
    const card = await screen.findByRole('article', { name: 'Summer Software Internship' })
    const save = within(card).getByRole('button', { name: 'Save' })
    expect(save).toBeDisabled()
    expect(save).toHaveAccessibleDescription(/discovery screens \(#22, #23\)/)
  })

  it('reaches another section with the keyboard only', async () => {
    renderDiscover()
    const nav = screen.getByRole('navigation', { name: 'Student sections' })
    const saved = within(nav).getByRole('link', { name: 'Saved opportunities' })
    while (document.activeElement !== saved) await userEvent.tab()
    await userEvent.keyboard('{Enter}')
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Saved opportunities' }),
    ).toBeInTheDocument()
    expect(screen.getByText(/built in issue #23/)).toBeInTheDocument()
  })

  it('has no axe violations', async () => {
    renderDiscover()
    await screen.findAllByRole('article')
    await screen.findByText('3 active')
    await expectNoAxeViolations()
  })
})
