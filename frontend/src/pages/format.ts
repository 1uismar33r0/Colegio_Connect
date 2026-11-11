/** Listing dates follow Puerto Rico's calendar, as the backend's deadline filter does. */
export const PUERTO_RICO_TIME_ZONE = 'America/Puerto_Rico'

const dateFormat = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  timeZone: PUERTO_RICO_TIME_ZONE,
})

/** "Nov 8" (Puerto Rico date) for an ISO instant. Shown next to a machine-readable <time dateTime>. */
export const shortDate = (iso: string) => dateFormat.format(new Date(iso))

/** Counts in stat tiles keep the wireframe's two-digit style ("08"). */
export const twoDigits = (n: number) => String(n).padStart(2, '0')
