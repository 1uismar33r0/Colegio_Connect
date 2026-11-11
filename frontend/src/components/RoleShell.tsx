import { NavLink, Outlet } from 'react-router'

export type RoleSection = { to: string; label: string }

type Props = {
  /** Accessible name of the section nav, e.g. "Student sections". */
  label: string
  /** The first section is the area's home page and only matches its exact path. */
  sections: RoleSection[]
}

/** Wireframe shared pattern: a sidebar of the area's sections next to the current page. */
export function RoleShell({ label, sections }: Props) {
  return (
    <div className="role-shell">
      <nav className="role-nav" aria-label={label}>
        <ul>
          {sections.map((s, i) => (
            <li key={s.to}>
              {/* NavLink sets aria-current="page" on the current section. */}
              <NavLink to={s.to} end={i === 0}>
                {s.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="role-content">
        <Outlet />
      </div>
    </div>
  )
}
