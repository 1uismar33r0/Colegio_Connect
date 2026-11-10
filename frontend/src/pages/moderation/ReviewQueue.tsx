import type { ReviewReason } from '../../api/contracts'
import { getReviewQueue } from '../../api/roleData'
import { useApiData } from '../../api/useApiData'
import { AsyncContent } from '../../components/AsyncContent'
import { EmptyState, PageHeading, Panel, StatTile, StatTiles } from '../../components/PagePieces'
import { shortDate, twoDigits } from '../format'

const REASON_LABEL: Record<ReviewReason, string> = {
  NEW_ORGANIZATION: 'New organization',
  REPORTED_LISTING: 'Reported listing',
  PROFILE_VERIFICATION: 'Profile verification',
}

const PENDING_NOTE_ID = 'review-pending-note'

export function ReviewQueue() {
  const queue = useApiData(getReviewQueue)

  return (
    <>
      <PageHeading
        title="Review queue"
        description="Triage submissions and keep the community safe."
      />
      <p id={PENDING_NOTE_ID} className="hint">
        Review actions open with the moderation tools (#16, #39).
      </p>

      <AsyncContent result={queue} loadingLabel="Loading the review queue…">
        {({ stats, items }) => (
          <>
            <StatTiles label="Queue summary">
              <StatTile label="Awaiting review" value={twoDigits(stats.awaitingReview)} />
              <StatTile label="Reported listings" value={twoDigits(stats.reportedListings)} />
              <StatTile label="Pending verification" value={twoDigits(stats.pendingVerification)} />
            </StatTiles>

            <Panel title="Items requiring review" headingId="review-items-heading">
              {items.length === 0 ? (
                <EmptyState>Nothing is waiting for review.</EmptyState>
              ) : (
                <div className="table-wrap">
                  <table aria-labelledby="review-items-heading">
                    <thead>
                      <tr>
                        <th scope="col">Submission</th>
                        <th scope="col">Submitted by</th>
                        <th scope="col">Reason</th>
                        <th scope="col">Received</th>
                        <th scope="col">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.map((item) => (
                        <tr key={item.id}>
                          <td>{item.submission}</td>
                          <td>{item.submittedBy}</td>
                          <td>{REASON_LABEL[item.reason]}</td>
                          <td>
                            <time dateTime={item.receivedAt}>{shortDate(item.receivedAt)}</time>
                          </td>
                          <td>
                            <div className="inline-actions">
                              <button
                                type="button"
                                className="secondary small"
                                disabled
                                aria-describedby={PENDING_NOTE_ID}
                                aria-label={`Review ${item.submission}`}
                              >
                                Review
                              </button>
                              <button
                                type="button"
                                className="secondary small"
                                disabled
                                aria-describedby={PENDING_NOTE_ID}
                                aria-label={`More actions for ${item.submission}`}
                              >
                                More
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Panel>
          </>
        )}
      </AsyncContent>
    </>
  )
}
