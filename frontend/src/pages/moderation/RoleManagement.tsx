import { useCallback, useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { assignRole, getRoleAssignments, revokeRole, type RoleAssignment } from '../../api/admin'
import { useApiData } from '../../api/useApiData'
import { ASSIGNABLE_ROLES, type AssignableRole, type Role } from '../../auth/roles'
import { describeFailure, useFormSubmit, ValidationFailure } from '../../auth/useFormSubmit'
import { AsyncContent } from '../../components/AsyncContent'
import { ErrorMessage } from '../../components/ErrorMessage'
import { FormField } from '../../components/FormField'
import { Notice } from '../../components/Notice'
import { EmptyState, PageHeading, Panel } from '../../components/PagePieces'

const ROLE_LABEL: Record<Role, string> = {
  STUDENT: 'Student',
  INSTITUTIONAL: 'University office or department',
  COMPANY: 'External organization',
  MODERATOR: 'Administrator / moderator',
}

const UUID_SHAPE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const isAssignable = (role: Role | undefined): role is AssignableRole =>
  (ASSIGNABLE_ROLES as readonly string[]).includes(role ?? '')

function uuidError(value: string, what: string): string | undefined {
  if (!value) return `Enter the ${what}.`
  if (!UUID_SHAPE.test(value))
    return `Enter the full ${what}, like 0190a1b2-c3d4-7e5f-8a9b-0c1d2e3f4a5b.`
  return undefined
}

/**
 * Wireframe "Accounts": assign and revoke INSTITUTIONAL and MODERATOR roles through
 * /api/admin/role-assignments (#10). The API has no user search yet, so accounts are looked up by
 * user ID; the audit log links here with ?userId=.
 */
export function RoleManagement() {
  const [params, setParams] = useSearchParams()
  const userId = params.get('userId') ?? ''
  const validUserId = UUID_SHAPE.test(userId) ? userId.toLowerCase() : ''

  return (
    <>
      <PageHeading
        title="Accounts"
        description="Assign and revoke office and administrator roles."
      />
      <UserLookup key={userId} initial={userId} onFind={(id) => setParams({ userId: id })} />
      {validUserId && <UserRoles key={validUserId} userId={validUserId} />}
    </>
  )
}

function UserLookup({ initial, onFind }: { initial: string; onFind: (id: string) => void }) {
  const [error, setError] = useState<string>()
  const inputRef = useRef<HTMLFormElement>(null)

  const submit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const value = String(new FormData(e.currentTarget).get('userId') ?? '').trim()
    const problem = uuidError(value, 'user ID')
    setError(problem)
    if (problem) {
      inputRef.current?.querySelector<HTMLElement>('input')?.focus()
      return
    }
    onFind(value.toLowerCase())
  }

  return (
    <form
      ref={inputRef}
      className="lookup"
      onSubmit={submit}
      noValidate
      aria-label="Find an account"
    >
      <FormField
        label="User ID"
        name="userId"
        defaultValue={initial}
        hint="Copy it from the audit log. Searching by email is not available yet."
        autoComplete="off"
        spellCheck={false}
        error={error}
      />
      <button type="submit">Show roles</button>
    </form>
  )
}

function UserRoles({ userId }: { userId: string }) {
  const load = useCallback(() => getRoleAssignments(userId), [userId])
  const assignments = useApiData(load)
  const [notice, setNotice] = useState<string>()

  const changed = (message: string) => {
    setNotice(message)
    assignments.reload()
  }

  return (
    <>
      {notice && <Notice>{notice}</Notice>}
      <Panel title="Current roles" headingId="current-roles-heading">
        <p className="hint">
          Account <code>{userId}</code>
        </p>
        <AsyncContent result={assignments} loadingLabel="Loading roles…">
          {(list) =>
            list.length === 0 ? (
              <EmptyState>This account has no roles yet.</EmptyState>
            ) : (
              <AssignmentTable list={list} onRevoked={() => changed('Role revoked.')} />
            )
          }
        </AsyncContent>
      </Panel>
      <AssignForm userId={userId} onAssigned={() => changed('Role assigned.')} />
    </>
  )
}

function AssignmentTable({ list, onRevoked }: { list: RoleAssignment[]; onRevoked: () => void }) {
  const [confirming, setConfirming] = useState<RoleAssignment>()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()
  const confirmRef = useRef<HTMLButtonElement>(null)

  // Move focus into the confirmation so keyboard users land on the decision.
  useEffect(() => {
    if (confirming) confirmRef.current?.focus()
  }, [confirming])

  const revoke = async () => {
    if (!confirming?.id || busy) return
    setBusy(true)
    setError(undefined)
    try {
      await revokeRole(confirming.id)
      setConfirming(undefined)
      onRevoked()
    } catch (err) {
      setError(describeFailure(err).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      {error && <ErrorMessage title="We couldn't revoke the role" message={error} />}
      <div className="table-wrap">
        <table aria-labelledby="current-roles-heading">
          <thead>
            <tr>
              <th scope="col">Role</th>
              <th scope="col">Organization</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {list.map((a) => (
              <tr key={a.id}>
                <td>{a.role ? ROLE_LABEL[a.role] : '—'}</td>
                <td>{a.organizationId ? <code>{a.organizationId}</code> : 'Platform-wide'}</td>
                <td>
                  {isAssignable(a.role) ? (
                    <button
                      type="button"
                      className="secondary small"
                      onClick={() => {
                        setError(undefined)
                        setConfirming(a)
                      }}
                      aria-label={`Revoke ${ROLE_LABEL[a.role]}`}
                    >
                      Revoke
                    </button>
                  ) : (
                    <span className="hint">Not managed here</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {confirming?.role && (
        <div className="confirm" role="group" aria-labelledby="confirm-revoke-text">
          <p id="confirm-revoke-text">
            Revoke <strong>{ROLE_LABEL[confirming.role]}</strong> from this account? It takes effect
            on the account's next request.
          </p>
          <div className="inline-actions">
            <button
              ref={confirmRef}
              type="button"
              aria-disabled={busy}
              onClick={() => void revoke()}
            >
              {busy ? 'Revoking…' : 'Confirm revoke'}
            </button>
            <button type="button" className="secondary" onClick={() => setConfirming(undefined)}>
              Cancel
            </button>
          </div>
        </div>
      )}
    </>
  )
}

function AssignForm({ userId, onAssigned }: { userId: string; onAssigned: () => void }) {
  const id = useId()
  const [role, setRole] = useState<AssignableRole>('INSTITUTIONAL')
  const { busy, failure, formRef, run } = useFormSubmit()
  const errors = failure?.fieldErrors ?? {}

  const submit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (busy) return
    const form = e.currentTarget
    const organizationId = String(new FormData(form).get('organizationId') ?? '').trim()

    void run(async () => {
      if (role === 'INSTITUTIONAL') {
        const problem = uuidError(organizationId, 'organization ID')
        if (problem) throw new ValidationFailure({ organizationId: problem })
      }
      await assignRole({
        userId,
        role,
        organizationId: role === 'INSTITUTIONAL' ? organizationId.toLowerCase() : undefined,
      })
      form.reset()
      onAssigned()
    })
  }

  return (
    <Panel title="Assign a role" headingId="assign-role-heading">
      <form ref={formRef} onSubmit={submit} noValidate aria-labelledby="assign-role-heading">
        {failure && <ErrorMessage title="We couldn't assign the role" message={failure.message} />}
        <div className="field">
          <label htmlFor={`${id}-role`}>Role</label>
          <select
            id={`${id}-role`}
            name="role"
            value={role}
            onChange={(e) => setRole(e.target.value as AssignableRole)}
          >
            {ASSIGNABLE_ROLES.map((r) => (
              <option key={r} value={r}>
                {ROLE_LABEL[r]}
              </option>
            ))}
          </select>
        </div>
        {role === 'INSTITUTIONAL' && (
          <FormField
            label="Organization ID"
            name="organizationId"
            hint="The UPRM office or department this role belongs to."
            autoComplete="off"
            spellCheck={false}
            required
            error={errors.organizationId}
          />
        )}
        <p className="hint">
          Only verified @upr.edu accounts can receive these roles. Student and company roles are not
          assigned here.
        </p>
        <button type="submit" aria-disabled={busy}>
          {busy ? 'Assigning…' : 'Assign role'}
        </button>
      </form>
    </Panel>
  )
}
