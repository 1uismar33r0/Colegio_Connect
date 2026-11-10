import type { ReactNode } from 'react'

/** A polite status message (success, info) that screen readers announce. */
export function Notice({ children }: { children: ReactNode }) {
  return (
    <div className="notice" role="status">
      {children}
    </div>
  )
}
