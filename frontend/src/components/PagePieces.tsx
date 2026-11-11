import type { ReactNode } from 'react'

/** Page title with its description and, per the wireframes, the primary action at the top right. */
export function PageHeading({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: ReactNode
}) {
  return (
    <div className="page-heading">
      <div>
        <h1>{title}</h1>
        <p className="hint">{description}</p>
      </div>
      {action && <div className="page-action">{action}</div>}
    </div>
  )
}

/** A labelled count. Rendered as a list item so the tiles read as one group. */
export function StatTile({ label, value }: { label: string; value: ReactNode }) {
  return (
    <li className="stat-tile">
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
    </li>
  )
}

export function StatTiles({ label, children }: { label: string; children: ReactNode }) {
  return (
    <ul className="stat-tiles" aria-label={label}>
      {children}
    </ul>
  )
}

export function StatusPill({ children }: { children: ReactNode }) {
  return <span className="status-pill">{children}</span>
}

/** A titled section of a page. The heading names the region for screen readers. */
export function Panel({
  title,
  headingId,
  children,
}: {
  title: string
  headingId: string
  children: ReactNode
}) {
  return (
    <section className="panel" aria-labelledby={headingId}>
      <h2 id={headingId}>{title}</h2>
      {children}
    </section>
  )
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <p className="empty-state">{children}</p>
}
