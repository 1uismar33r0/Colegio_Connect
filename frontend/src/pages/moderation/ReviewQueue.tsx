import { useCallback, useState } from 'react'
import { getPendingCompanies } from '../../api/admin'
import { useApiData } from '../../api/useApiData'
import { AsyncContent } from '../../components/AsyncContent'
import {
  EmptyState,
  PageHeading,
  Panel,
  StatTile,
  StatTiles,
  StatusPill,
} from '../../components/PagePieces'
import { Pager } from '../../components/Pager'
import { twoDigits } from '../format'

const PENDING_NOTE_ID = 'review-pending-note'

/**
 * Wireframe "Review queue". Uses the real pending-company queue (GET /api/admin/organizations/pending,
 * #16). Reported listings arrive with reporting (#37); until then they are shown as not available.
 */
export function ReviewQueue() {
  const [page, setPage] = useState(0)
  const load = useCallback(() => getPendingCompanies(page), [page])
  const queue = useApiData(load)

  return (
    <>
      <PageHeading
        title="Review queue"
        description="Triage submissions and keep the community safe."
      />
      <p id={PENDING_NOTE_ID} className="hint">
        Approve and reject actions open with the company approval screens (#16). Reported listings
        arrive with reporting (#37).
      </p>

      <AsyncContent result={queue} loadingLabel="Loading the review queue…">
        {({ items = [], totalElements = 0, totalPages = 0 }) => (
          <>
            <StatTiles label="Queue summary">
              <StatTile label="Awaiting review" value={twoDigits(totalElements)} />
              <StatTile
                label="Reported listings"
                value={<span aria-label="Not available yet">—</span>}
              />
              <StatTile label="Pending verification" value={twoDigits(totalElements)} />
            </StatTiles>

            <Panel title="Items requiring review" headingId="review-items-heading">
              {items.length === 0 ? (
                <EmptyState>Nothing is waiting for review.</EmptyState>
              ) : (
                <>
                  <div className="table-wrap">
                    <table aria-labelledby="review-items-heading">
                      <thead>
                        <tr>
                          <th scope="col">Organization</th>
                          <th scope="col">Reason</th>
                          <th scope="col">Status</th>
                          <th scope="col">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {items.map((org) => (
                          <tr key={org.id}>
                            <td>{org.name}</td>
                            <td>New organization</td>
                            <td>
                              <StatusPill>Pending</StatusPill>
                            </td>
                            <td>
                              <button
                                type="button"
                                className="secondary small"
                                disabled
                                aria-describedby={PENDING_NOTE_ID}
                                aria-label={`Review ${org.name}`}
                              >
                                Review
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <Pager
                    label="Review queue pages"
                    page={page}
                    totalPages={totalPages}
                    onChange={setPage}
                  />
                </>
              )}
            </Panel>
          </>
        )}
      </AsyncContent>
    </>
  )
}
