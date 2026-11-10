const dateFormat = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' })

/** "Nov 8" for an ISO instant. Shown next to a full machine-readable <time dateTime>. */
export const shortDate = (iso: string) => dateFormat.format(new Date(iso))

/** Counts in stat tiles keep the wireframe's two-digit style ("08"). */
export const twoDigits = (n: number) => String(n).padStart(2, '0')
