export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div role="status" aria-live="polite">
      <p>{label}</p>
    </div>
  )
}
