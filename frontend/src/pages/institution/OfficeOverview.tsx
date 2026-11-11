import { getOfficeOverview, type ListingStatus } from '../../api/roleData'
import { OrganizationScope } from '../../components/OrganizationScope'
import {
  EmptyState,
  PageHeading,
  Panel,
  StatTile,
  StatTiles,
  StatusPill,
} from '../../components/PagePieces'
import { twoDigits } from '../format'

const STATUS_LABEL: Record<ListingStatus, string> = {
  PUBLISHED: 'Published',
  DRAFT: 'Draft',
  CLOSED: 'Closed',
}

const PENDING_NOTE_ID = 'office-pending-note'

export function OfficeOverview() {
  return (
    <>
      <PageHeading
        title="Office overview"
        description="Manage opportunities and connect with students."
        action={
          <button type="button" disabled aria-describedby={PENDING_NOTE_ID}>
            + Create opportunity
          </button>
        }
      />
      <p id={PENDING_NOTE_ID} className="hint">
        Creating and editing opportunities opens with the publisher screens (#20).
      </p>

      <OrganizationScope
        kind="office or department"
        load={getOfficeOverview}
        loadingLabel="Loading your office overview…"
      >
        {({ stats, recent }) => (
          <>
            <StatTiles label="Office summary">
              <StatTile label="Published opportunities" value={twoDigits(stats.published)} />
              <StatTile label="New responses" value={twoDigits(stats.newResponses)} />
              <StatTile label="Drafts" value={twoDigits(stats.drafts)} />
            </StatTiles>

            <Panel title="Recent opportunities" headingId="office-recent-heading">
              {recent.length === 0 ? (
                <EmptyState>You haven't created any opportunities yet.</EmptyState>
              ) : (
                <div className="table-wrap">
                  <table aria-labelledby="office-recent-heading">
                    <thead>
                      <tr>
                        <th scope="col">Opportunity</th>
                        <th scope="col">Type</th>
                        <th scope="col">Responses</th>
                        <th scope="col">Status</th>
                        <th scope="col">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recent.map((l) => (
                        <tr key={l.id}>
                          <td>{l.title}</td>
                          <td>{l.type}</td>
                          <td>{l.newResponses === null ? '—' : `${l.newResponses} new`}</td>
                          <td>
                            <StatusPill>{STATUS_LABEL[l.status]}</StatusPill>
                          </td>
                          <td>
                            <button
                              type="button"
                              className="secondary small"
                              disabled
                              aria-describedby={PENDING_NOTE_ID}
                              aria-label={`${l.status === 'DRAFT' ? 'Edit' : 'Manage'} ${l.title}`}
                            >
                              {l.status === 'DRAFT' ? 'Edit' : 'Manage'}
                            </button>
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
      </OrganizationScope>
    </>
  )
}
