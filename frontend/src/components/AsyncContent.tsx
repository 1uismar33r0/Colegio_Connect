import type { ReactNode } from 'react'
import { ApiError } from '../api/problem'
import type { ApiData } from '../api/useApiData'
import { ErrorMessage } from './ErrorMessage'
import { Loading } from './Loading'

/** Plain-language message only; technical details never reach the UI. */
function messageFor(error: Error) {
  if (error instanceof ApiError && error.status === 403) {
    return {
      title: "You don't have access to this information",
      message: 'If you think this is a mistake, contact an administrator.',
      retry: false,
    }
  }
  return {
    title: "We couldn't load this information",
    message: 'Check your connection and try again.',
    retry: true,
  }
}

/** Shows loading and error states for a useApiData result, and children(data) on success. */
export function AsyncContent<T>({
  result,
  loadingLabel,
  children,
}: {
  result: ApiData<T>
  loadingLabel: string
  children: (data: T) => ReactNode
}) {
  if (result.status === 'loading') return <Loading label={loadingLabel} />
  if (result.status === 'error') {
    const { title, message, retry } = messageFor(result.error)
    return (
      <ErrorMessage title={title} message={message} onRetry={retry ? result.reload : undefined} />
    )
  }
  return <>{children(result.data)}</>
}
