import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useApiData } from './useApiData'

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((r) => (resolve = r))
  return { promise, resolve }
}

describe('useApiData', () => {
  it('starts loading, then returns the data', async () => {
    const load = vi.fn(async () => 'data')
    const { result } = renderHook(() => useApiData(load))
    expect(result.current.status).toBe('loading')
    await waitFor(() => expect(result.current).toMatchObject({ status: 'success', data: 'data' }))
  })

  it('returns errors and loads again on reload()', async () => {
    const load = vi.fn<() => Promise<string>>().mockRejectedValueOnce(new Error('boom'))
    load.mockResolvedValue('ok')
    const { result } = renderHook(() => useApiData(load))
    await waitFor(() => expect(result.current.status).toBe('error'))

    act(() => result.current.reload())
    expect(result.current.status).toBe('loading')
    await waitFor(() => expect(result.current).toMatchObject({ status: 'success', data: 'ok' }))
    expect(load).toHaveBeenCalledTimes(2)
  })

  it('ignores a late response from an earlier load', async () => {
    const first = deferred<string>()
    const second = deferred<string>()
    const loadFirst = () => first.promise
    const loadSecond = () => second.promise

    const { result, rerender } = renderHook(({ load }) => useApiData(load), {
      initialProps: { load: loadFirst },
    })
    rerender({ load: loadSecond })
    await act(async () => first.resolve('stale'))
    expect(result.current.status).toBe('loading')

    await act(async () => second.resolve('fresh'))
    expect(result.current).toMatchObject({ status: 'success', data: 'fresh' })
  })
})
