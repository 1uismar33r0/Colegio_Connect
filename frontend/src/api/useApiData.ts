import { useCallback, useEffect, useState } from 'react'

export type ApiData<T> =
  | { status: 'loading'; reload: () => void }
  | { status: 'success'; data: T; reload: () => void }
  | { status: 'error'; error: Error; reload: () => void }

type Settled<T> = { load: () => Promise<T>; attempt: number } & (
  { ok: true; data: T } | { ok: false; error: Error }
)

/**
 * Runs `load` on mount, whenever `load` changes (memoize it with useCallback when it depends
 * on state) and on reload(). A result only counts for the load/attempt that produced it, so
 * a late response from an earlier request never overwrites a newer one.
 */
export function useApiData<T>(load: () => Promise<T>): ApiData<T> {
  const [attempt, setAttempt] = useState(0)
  const [settled, setSettled] = useState<Settled<T> | null>(null)

  useEffect(() => {
    let cancelled = false
    load().then(
      (data) => {
        if (!cancelled) setSettled({ load, attempt, ok: true, data })
      },
      (err: unknown) => {
        const error = err instanceof Error ? err : new Error(String(err))
        if (!cancelled) setSettled({ load, attempt, ok: false, error })
      },
    )
    return () => {
      cancelled = true
    }
  }, [load, attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  const current = settled && settled.load === load && settled.attempt === attempt ? settled : null
  if (!current) return { status: 'loading', reload }
  return current.ok
    ? { status: 'success', data: current.data, reload }
    : { status: 'error', error: current.error, reload }
}
