import { useCallback, useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import type { FeedItem, StudentFeedFilters } from '../../api/contracts'
import { getStudentActivity, getStudentFeed } from '../../api/roleData'
import { useApiData } from '../../api/useApiData'
import { AsyncContent } from '../../components/AsyncContent'
import { EmptyState, PageHeading, Panel, StatusPill } from '../../components/PagePieces'
import { shortDate } from '../format'

// Wireframe placeholders; the real categories and fields come from the listing model (#18).
const TYPE_OPTIONS = ['Internship', 'Capstone sponsorship', 'Event']
const FIELD_OPTIONS = ['Technology', 'Community']

const PENDING_NOTE_ID = 'discover-pending-note'

export function StudentDiscover() {
  const [filters, setFilters] = useState<StudentFeedFilters>({})
  const loadFeed = useCallback(() => getStudentFeed(filters), [filters])
  const feed = useApiData(loadFeed)
  const activity = useApiData(getStudentActivity)

  return (
    <>
      <PageHeading
        title="Find your next opportunity"
        description="Explore internships, projects, events, and capstone sponsorships."
        action={
          <Link className="button-link secondary" to="/student/responses">
            View my responses
          </Link>
        }
      />

      <FeedFilters onApply={setFilters} />

      <div className="columns">
        <section aria-labelledby="feed-heading">
          <h2 id="feed-heading">Opportunities</h2>
          <p id={PENDING_NOTE_ID} className="hint">
            Listing details and saving open with the discovery screens (#22, #23).
          </p>
          <AsyncContent result={feed} loadingLabel="Loading opportunities…">
            {(items) =>
              items.length === 0 ? (
                <EmptyState>No opportunities match these filters yet.</EmptyState>
              ) : (
                <ul className="card-list">
                  {items.map((item) => (
                    <li key={item.id}>
                      <OpportunityCard item={item} />
                    </li>
                  ))}
                </ul>
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
  const [keyword, setKeyword] = useState('')
  const [type, setType] = useState('')
  const [field, setField] = useState('')

  const submit = (e: FormEvent) => {
    e.preventDefault()
    onApply({
      keyword: keyword.trim() || undefined,
      type: type || undefined,
      field: field || undefined,
    })
  }

  return (
    <form className="filters" role="search" aria-label="Filter opportunities" onSubmit={submit}>
      <div className="filter">
        <label htmlFor={`${id}-keyword`}>Keyword</label>
        <input
          id={`${id}-keyword`}
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Search by keyword"
        />
      </div>
      <div className="filter">
        <label htmlFor={`${id}-type`}>Opportunity type</label>
        <select id={`${id}-type`} value={type} onChange={(e) => setType(e.target.value)}>
          <option value="">All opportunity types</option>
          {TYPE_OPTIONS.map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
      </div>
      <div className="filter">
        <label htmlFor={`${id}-field`}>Field of interest</label>
        <select id={`${id}-field`} value={field} onChange={(e) => setField(e.target.value)}>
          <option value="">All fields</option>
          {FIELD_OPTIONS.map((f) => (
            <option key={f}>{f}</option>
          ))}
        </select>
      </div>
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
