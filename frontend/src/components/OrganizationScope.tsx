import { useCallback, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import { isUuid, uuidError } from '../api/ids'
import { ApiError } from '../api/problem'
import { useApiData } from '../api/useApiData'
import { AsyncContent } from './AsyncContent'
import { FormField } from './FormField'
import { Notice } from './Notice'
import { Panel } from './PagePieces'

type Props<T> = {
  /** What the user belongs to, e.g. "office or department". */
  kind: string
  load: (organizationId?: string) => Promise<T>
  loadingLabel: string
  children: (data: T) => ReactNode
}

/**
 * Loads a dashboard that serves one organization at a time. The choice lives in
 * ?organizationId=. Without it the backend uses the user's only eligible organization, and
 * answers 409 when there are several; then the user chooses one here. The frontend never picks
 * one itself.
 *
 * No endpoint lists a user's organizations yet, so the choice is an organization ID the user
 * enters (backend dependency recorded in TE-14).
 */
export function OrganizationScope<T>(props: Props<T>) {
  const [params, setParams] = useSearchParams()
  const raw = (params.get('organizationId') ?? '').trim()
  const organizationId = isUuid(raw) ? raw.toLowerCase() : undefined
  const select = useCallback((id: string) => setParams({ organizationId: id }), [setParams])

  if (raw && !organizationId) {
    return (
      <OrganizationPicker
        kind={props.kind}
        reason="That organization ID isn't valid. Enter the full ID of the organization to open."
        onSelect={select}
      />
    )
  }
  return (
    <ScopedData
      key={organizationId ?? ''}
      {...props}
      organizationId={organizationId}
      onSelect={select}
    />
  )
}

function ScopedData<T>({
  kind,
  load,
  loadingLabel,
  children,
  organizationId,
  onSelect,
}: Props<T> & { organizationId?: string; onSelect: (id: string) => void }) {
  const loadSelected = useCallback(() => load(organizationId), [load, organizationId])
  const result = useApiData(loadSelected)

  if (
    result.status === 'error' &&
    result.error instanceof ApiError &&
    result.error.status === 409
  ) {
    return (
      <OrganizationPicker
        kind={kind}
        reason={`Your account is linked to more than one ${kind}. Choose which one to open.`}
        onSelect={onSelect}
      />
    )
  }
  return (
    <>
      {organizationId && (
        <p className="hint scope">
          Showing organization <code>{organizationId}</code>.{' '}
          <Link to={{ search: '' }}>Choose another organization</Link>
        </p>
      )}
      <AsyncContent result={result} loadingLabel={loadingLabel}>
        {children}
      </AsyncContent>
    </>
  )
}

function OrganizationPicker({
  kind,
  reason,
  onSelect,
}: {
  kind: string
  reason: string
  onSelect: (id: string) => void
}) {
  const [error, setError] = useState<string>()
  const formRef = useRef<HTMLFormElement>(null)
  const focusInput = () => formRef.current?.querySelector<HTMLElement>('input')?.focus()

  const submit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const value = String(new FormData(e.currentTarget).get('organizationId') ?? '').trim()
    const problem = uuidError(value, 'organization ID')
    setError(problem)
    if (problem) focusInput()
    else onSelect(value.toLowerCase())
  }

  return (
    <Panel title="Choose an organization" headingId="organization-picker-heading">
      <Notice>{reason}</Notice>
      <form ref={formRef} className="lookup" onSubmit={submit} noValidate>
        <FormField
          label="Organization ID"
          name="organizationId"
          autoComplete="off"
          spellCheck={false}
          hint={`Colegio Connect can't list your organizations yet. Ask an administrator for the ID of the ${kind} you want to open.`}
          error={error}
        />
        <button type="submit">Open dashboard</button>
      </form>
    </Panel>
  )
}
