import { getCompanyDashboard, type ApprovalStatus } from '../../api/roleData'
import { Notice } from '../../components/Notice'
import { OrganizationScope } from '../../components/OrganizationScope'
import {
  EmptyState,
  PageHeading,
  Panel,
  StatTile,
  StatTiles,
  StatusPill,
} from '../../components/PagePieces'
import { shortDate, twoDigits } from '../format'

// The organization's persisted approval state (#16), as the backend reports it.
const APPROVAL_LABEL: Record<ApprovalStatus, string> = {
  PENDING: 'Pending approval',
  APPROVED: 'Approved',
  REJECTED: 'Not approved',
}

const PENDING_NOTE_ID = 'company-pending-note'

export function OrgDashboard() {
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

      <OrganizationScope
        kind="organization"
        load={getCompanyDashboard}
        loadingLabel="Loading your organization dashboard…"
      >
        {({ stats, active, attention }) => (
          <>
            {stats.approvalStatus === 'PENDING' && (
              <Notice>
                Your organization is waiting for administrator approval. You can view this
                dashboard, but you can't post opportunities until it's approved.
              </Notice>
            )}
            {stats.approvalStatus === 'REJECTED' && (
              <Notice>
                Your organization's registration was not approved. Contact an administrator if you
                think this is a mistake.
              </Notice>
            )}

            <StatTiles label="Organization summary">
              <StatTile label="Active listings" value={twoDigits(stats.activeListings)} />
              <StatTile label="Student responses" value={twoDigits(stats.responses)} />
              <StatTile label="Approval status" value={APPROVAL_LABEL[stats.approvalStatus]} />
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
                      <strong>Organization approval</strong>
                      <span className="hint">Reviewed by a Colegio Connect administrator</span>
                    </div>
                    <StatusPill>{APPROVAL_LABEL[stats.approvalStatus]}</StatusPill>
                  </li>
                </ul>
              </Panel>
            </div>
          </>
        )}
      </OrganizationScope>
    </>
  )
}
