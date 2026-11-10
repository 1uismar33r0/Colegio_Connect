import { useCallback, useState } from 'react'
import { Link } from 'react-router'
import { getAuditEvents, type AuditEvent } from '../../api/admin'
import { useApiData } from '../../api/useApiData'
import { AsyncContent } from '../../components/AsyncContent'
import { EmptyState, PageHeading, Panel } from '../../components/PagePieces'
import { Pager } from '../../components/Pager'

const ACTION_LABEL: Record<NonNullable<AuditEvent['action']>, string> = {
  ROLE_ASSIGNED: 'Role assigned',
  ROLE_REVOKED: 'Role revoked',
  ORGANIZATION_APPROVED: 'Organization approved',
  ORGANIZATION_REJECTED: 'Organization rejected',
  LISTING_HIDDEN: 'Listing hidden',
  LISTING_REMOVED: 'Listing removed',
  ACCOUNT_SUSPENDED: 'Account suspended',
  SPONSORSHIP_ACCEPTED: 'Sponsorship accepted',
}

const TARGET_LABEL: Record<NonNullable<AuditEvent['targetType']>, string> = {
  USER: 'Account',
  ORGANIZATION: 'Organization',
  LISTING: 'Listing',
  SPONSORSHIP_REQUEST: 'Sponsorship request',
}

const timestamp = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' })

/** Newest-first audit history (GET /api/admin/audit-events, #15). */
export function AuditLog() {
  const [page, setPage] = useState(0)
  const load = useCallback(() => getAuditEvents(page), [page])
  const events = useApiData(load)

  return (
    <>
      <PageHeading
        title="Audit log"
        description="Who changed roles, approvals and moderation state, and when."
      />
      <AsyncContent result={events} loadingLabel="Loading the audit log…">
        {({ items = [], totalPages = 0 }) => (
          <Panel title="Recent events" headingId="audit-events-heading">
            {items.length === 0 ? (
              <EmptyState>No audited actions yet.</EmptyState>
            ) : (
              <>
                <div className="table-wrap">
                  <table aria-labelledby="audit-events-heading">
                    <thead>
                      <tr>
                        <th scope="col">When</th>
                        <th scope="col">Action</th>
                        <th scope="col">Target</th>
                        <th scope="col">Actor ID</th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.map((e) => (
                        <tr key={e.id}>
                          <td>
                            {e.occurredAt && (
                              <time dateTime={e.occurredAt}>
                                {timestamp.format(new Date(e.occurredAt))}
                              </time>
                            )}
                          </td>
                          <td>{e.action ? ACTION_LABEL[e.action] : '—'}</td>
                          <td>
                            {e.targetType ? TARGET_LABEL[e.targetType] : '—'}{' '}
                            <code>{e.targetId}</code>
                            {e.targetType === 'USER' && e.targetId && (
                              <>
                                {' '}
                                <Link
                                  to={`/moderation/accounts?userId=${encodeURIComponent(e.targetId)}`}
                                  aria-label={`Manage roles for ${e.targetId}`}
                                >
                                  Manage roles
                                </Link>
                              </>
                            )}
                          </td>
                          <td>
                            <code>{e.actorId}</code>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <Pager
                  label="Audit log pages"
                  page={page}
                  totalPages={totalPages}
                  onChange={setPage}
                />
              </>
            )}
          </Panel>
        )}
      </AsyncContent>
    </>
  )
}
