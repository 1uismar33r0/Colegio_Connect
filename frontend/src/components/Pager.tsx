/** Previous/next for zero-based paged API responses. Hidden when there is only one page. */
export function Pager({
  label,
  page,
  totalPages,
  onChange,
}: {
  label: string
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null
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
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className="secondary small"
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        Next page
      </button>
    </nav>
  )
}
