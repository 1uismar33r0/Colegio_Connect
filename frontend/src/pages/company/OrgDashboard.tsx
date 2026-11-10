import type { ProfileStatus } from '../../api/contracts'
import { getCompanyDashboard } from '../../api/roleData'
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
import { shortDate, twoDigits } from '../format'

// "Profile status" is an open wireframe question; PENDING_APPROVAL covers the #16 workflow.
const PROFILE_LABEL: Record<ProfileStatus, string> = {
  COMPLETE: 'Complete',
  INCOMPLETE: 'Incomplete',
  PENDING_APPROVAL: 'Pending approval',
}

const PENDING_NOTE_ID = 'company-pending-note'

export function OrgDashboard() {
  const dashboard = useApiData(getCompanyDashboard)

  return (
    <>
      <PageHeading
        title="Organization dashboard"
        description="Share opportunities and manage student interest."
        action={
          <button type="button" disabled aria-describedby={PENDING_NOTE_ID}>
            + Post an opportunity
          </button>
        }
      />
      <p id={PENDING_NOTE_ID} className="hint">
        Posting opportunities opens with the publisher screens (#20) once your organization is
        approved.
      </p>

      <AsyncContent result={dashboard} loadingLabel="Loading your organization dashboard…">
        {({ stats, active, attention }) => (
          <>
            <StatTiles label="Organization summary">
              <StatTile label="Active listings" value={twoDigits(stats.activeListings)} />
              <StatTile label="Student responses" value={twoDigits(stats.responses)} />
              <StatTile label="Profile status" value={PROFILE_LABEL[stats.profileStatus]} />
            </StatTiles>

            <div className="columns">
              <Panel title="Active opportunities" headingId="company-active-heading">
                {active.length === 0 ? (
                  <EmptyState>You have no active opportunities.</EmptyState>
                ) : (
                  <ul className="rows">
                    {active.map((l) => (
                      <li key={l.id} className="row">
                        <div>
                          <strong>{l.title}</strong>
                          <span className="hint">
                            {l.type} · Closes{' '}
                            <time dateTime={l.closesAt}>{shortDate(l.closesAt)}</time>
                          </span>
                        </div>
                        <StatusPill>
                          {l.responses} {l.responses === 1 ? 'response' : 'responses'}
                        </StatusPill>
                      </li>
                    ))}
                  </ul>
                )}
              </Panel>

              <Panel title="Needs attention" headingId="company-attention-heading">
                <ul className="rows">
                  <li className="row">
                    <div>
                      <strong>New student responses</strong>
                      <span className="hint">Review and follow up</span>
                    </div>
                    <StatusPill>{attention.newResponses} new</StatusPill>
                  </li>
                  <li className="row">
                    <div>
                      <strong>Organization profile</strong>
                      <span className="hint">Keep your identity clear for students</span>
                    </div>
                    <StatusPill>{PROFILE_LABEL[stats.profileStatus]}</StatusPill>
                  </li>
                </ul>
              </Panel>
            </div>
          </>
        )}
      </AsyncContent>
    </>
  )
}
