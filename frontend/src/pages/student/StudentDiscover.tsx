import { useCallback, useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { uuidError } from '../../api/ids'
import { FEED_PAGE_SIZE, OPPORTUNITY_CATEGORIES, SOURCE_LABELS } from '../../api/listingOptions'
import {
  getStudentActivity,
  getStudentFeed,
  type FeedItem,
  type StudentFeedFilters,
} from '../../api/roleData'
import { useApiData } from '../../api/useApiData'
import { AsyncContent } from '../../components/AsyncContent'
import { EmptyState, PageHeading, Panel, StatusPill } from '../../components/PagePieces'
import { Pager } from '../../components/Pager'
import { shortDate } from '../format'

const PENDING_NOTE_ID = 'discover-pending-note'

export function StudentDiscover() {
  const [query, setQuery] = useState<{ filters: StudentFeedFilters; page: number }>({
    filters: {},
    page: 0,
  })
  const loadFeed = useCallback(() => getStudentFeed(query.filters, query.page), [query])
  const feed = useApiData(loadFeed)
  const activity = useApiData(getStudentActivity)
  const filtered = Object.values(query.filters).some(Boolean)

  return (
    <>
      <PageHeading
        title="Find your next opportunity"
        description="Explore jobs, internships, co-ops, research and capstone sponsorships."
        action={
          <Link className="button-link secondary" to="/student/responses">
            View my responses
          </Link>
        }
      />

      <FeedFilters onApply={(filters) => setQuery({ filters, page: 0 })} />

      <div className="columns">
        <section aria-labelledby="feed-heading">
          <h2 id="feed-heading">Opportunities</h2>
          <p id={PENDING_NOTE_ID} className="hint">
            Listing details and saving open with the discovery screens (#22, #23).
          </p>
          <AsyncContent result={feed} loadingLabel="Loading opportunities…">
            {(items) =>
              items.length === 0 ? (
                <EmptyState>
                  {query.page > 0
                    ? 'There are no more opportunities.'
                    : filtered
                      ? 'No opportunities match these filters.'
                      : 'There are no open opportunities right now.'}
                </EmptyState>
              ) : (
                <>
                  <ul className="card-list">
                    {items.map((item) => (
                      <li key={item.id}>
                        <OpportunityCard item={item} />
                      </li>
                    ))}
                  </ul>
                  <Pager
                    label="Opportunity pages"
                    page={query.page}
                    hasNext={items.length === FEED_PAGE_SIZE}
                    onChange={(page) => setQuery((q) => ({ ...q, page }))}
                  />
                </>
              )
            }
          </AsyncContent>
        </section>

        <div>
          <Panel title="My activity" headingId="activity-heading">
            <AsyncContent result={activity} loadingLabel="Loading your activity…">
              {(a) => (
                <ul className="rows">
                  <li className="row">
                    <div>
                      <strong>Responses</strong>
                      <span className="hint">Opportunities you've contacted</span>
                    </div>
                    <StatusPill>{a.activeResponses} active</StatusPill>
                  </li>
                  <li className="row">
                    <div>
                      <strong>Saved</strong>
                      <span className="hint">Come back to these later</span>
                    </div>
                    <StatusPill>{a.saved} saved</StatusPill>
                  </li>
                </ul>
              )}
            </AsyncContent>
          </Panel>
        </div>
      </div>
    </>
  )
}

function FeedFilters({ onApply }: { onApply: (filters: StudentFeedFilters) => void }) {
  const id = useId()
  const [organizationError, setOrganizationError] = useState<string>()

  const submit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = e.currentTarget
    const data = new FormData(form)
    const value = (name: string) => String(data.get(name) ?? '').trim() || undefined
    const organizationId = value('organizationId')?.toLowerCase()

    const problem = organizationId ? uuidError(organizationId, 'organization ID') : undefined
    setOrganizationError(problem)
    if (problem) {
      // Open the disclosure first; a field inside a closed <details> can't take focus.
      form.querySelector('details')?.setAttribute('open', '')
      form.querySelector<HTMLElement>('[name="organizationId"]')?.focus()
      return
    }
    onApply({
      keyword: value('keyword'),
      category: value('category'),
      sourceLabel: value('sourceLabel'),
      deadlineOnOrBefore: value('deadlineOnOrBefore'),
      organizationId,
    })
  }

  return (
    <form
      className="filters"
      role="search"
      aria-label="Filter opportunities"
      onSubmit={submit}
      noValidate
    >
      <div className="filter">
        <label htmlFor={`${id}-keyword`}>Keyword</label>
        <input id={`${id}-keyword`} name="keyword" type="search" placeholder="Search by keyword" />
      </div>
      <div className="filter">
        <label htmlFor={`${id}-category`}>Category</label>
        <select id={`${id}-category`} name="category" defaultValue="">
          <option value="">All categories</option>
          {OPPORTUNITY_CATEGORIES.map((c) => (
            <option key={c.value} value={c.value}>
              {c.label}
            </option>
          ))}
        </select>
      </div>
      <div className="filter">
        <label htmlFor={`${id}-source`}>Publisher</label>
        <select id={`${id}-source`} name="sourceLabel" defaultValue="">
          <option value="">All verified publishers</option>
          {SOURCE_LABELS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>
      <div className="filter">
        <label htmlFor={`${id}-deadline`}>Closes on or before</label>
        <input
          id={`${id}-deadline`}
          name="deadlineOnOrBefore"
          type="date"
          aria-describedby={`${id}-deadline-hint`}
        />
        <span id={`${id}-deadline-hint`} className="hint">
          Puerto Rico date
        </span>
      </div>
      <details className="filter more-filters">
        <summary>More filters</summary>
        <label htmlFor={`${id}-organization`}>Organization ID</label>
        <input
          id={`${id}-organization`}
          name="organizationId"
          autoComplete="off"
          spellCheck={false}
          aria-invalid={organizationError ? true : undefined}
          aria-describedby={organizationError ? `${id}-organization-error` : undefined}
        />
        {organizationError && (
          <span id={`${id}-organization-error`} className="field-error">
            {organizationError}
          </span>
        )}
      </details>
      <button type="submit" className="secondary">
        Apply filters
      </button>
    </form>
  )
}

function OpportunityCard({ item }: { item: FeedItem }) {
  const headingId = `opportunity-${item.id}`
  return (
    <article className="card" aria-labelledby={headingId}>
      <div className="card-head">
        <div>
          <h3 id={headingId}>{item.title}</h3>
          <p className="hint">
            {item.source} · Posted <time dateTime={item.postedAt}>{shortDate(item.postedAt)}</time>
          </p>
          <p className="trust-label">
            <span aria-hidden="true">✓ </span>
            {item.sourceLabel}
          </p>
        </div>
        <span className="tag">{item.type}</span>
      </div>
      <p>{item.summary}</p>
      {item.tags.length > 0 && (
        <ul className="tags" aria-label="Topics">
          {item.tags.map((tag) => (
            <li key={tag} className="tag">
              {tag}
            </li>
          ))}
        </ul>
      )}
      <div className="card-actions">
        <button type="button" disabled aria-describedby={PENDING_NOTE_ID}>
          View details
        </button>
        <button type="button" className="secondary" disabled aria-describedby={PENDING_NOTE_ID}>
          Save
        </button>
      </div>
    </article>
  )
}
