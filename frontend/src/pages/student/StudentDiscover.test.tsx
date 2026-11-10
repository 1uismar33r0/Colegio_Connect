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

const renderDiscover = () => renderWithAuth(routes, '/student', fakeAuth({ roles: ['STUDENT'] }))

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
    expect(card).toHaveTextContent('Capstone')
    expect(within(card).getByRole('list', { name: 'Topics' })).toHaveTextContent('Data science')
    expect(screen.getAllByRole('article')).toHaveLength(fixtures.feed.length)

    const activity = screen.getByRole('region', { name: 'My activity' })
    expect(await within(activity).findByText('3 active')).toBeInTheDocument()
    expect(within(activity).getByText('5 saved')).toBeInTheDocument()
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

  it('sends the chosen filters to the API', async () => {
    const api = mockRoleApi()
    renderDiscover()
    await screen.findAllByRole('article')

    api.getStudentFeed.mockImplementation(async () => [fixtures.feed[1]])
    await userEvent.type(screen.getByLabelText('Keyword'), '  software ')
    await userEvent.selectOptions(screen.getByLabelText('Opportunity type'), 'Internship')
    await userEvent.click(screen.getByRole('button', { name: 'Apply filters' }))

    expect(api.getStudentFeed).toHaveBeenLastCalledWith({
      keyword: 'software',
      type: 'Internship',
      field: undefined,
    })
    expect(
      await screen.findByRole('article', { name: 'Summer Software Internship' }),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('article')).toHaveLength(1)
  })

  it('shows an empty state when nothing matches', async () => {
    mockRoleApi({ getStudentFeed: async () => [] })
    renderDiscover()
    expect(await screen.findByText('No opportunities match these filters yet.')).toBeInTheDocument()
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
