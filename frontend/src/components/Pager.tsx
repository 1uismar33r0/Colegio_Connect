type Props = {
  label: string
  page: number
  onChange: (page: number) => void
} & (
  | { totalPages: number; hasNext?: never }
  /** For endpoints that return a plain list: a full page means there may be another. */
  | { totalPages?: never; hasNext: boolean }
)

/** Previous/next for zero-based paged API responses. Hidden when there is only one page. */
export function Pager({ label, page, totalPages, hasNext, onChange }: Props) {
  const nextExists = totalPages === undefined ? hasNext : page + 1 < totalPages
  if (page <= 0 && !nextExists) return null
  return (
    <nav className="pager" aria-label={label}>
      <button
        type="button"
        className="secondary small"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        Previous page
      </button>
      <span>
        {totalPages === undefined ? `Page ${page + 1}` : `Page ${page + 1} of ${totalPages}`}
      </span>
      <button
        type="button"
        className="secondary small"
        disabled={!nextExists}
        onClick={() => onChange(page + 1)}
      >
        Next page
      </button>
    </nav>
  )
}
