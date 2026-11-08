import { Outlet, useLocation } from 'react-router'

/** Stand-in destination page that shows the router state it received. */
export function Landing({ name }: { name: string }) {
  const state: unknown = useLocation().state
  return (
    <>
      <h1>{name}</h1>
      <pre data-testid="state">{JSON.stringify(state ?? null)}</pre>
    </>
  )
}

/** Gives pages the <main> landmark that AppLayout provides in the real app. */
export function TestMain() {
  return (
    <main>
      <Outlet />
    </main>
  )
}
