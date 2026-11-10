import type { ReactNode } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { RequireAuth, RequireRole } from './auth/guards'
import type { Role } from './auth/roles'
import { AppLayout, type RouteHandle } from './components/AppLayout'
import { RoleShell } from './components/RoleShell'
import { RouteError } from './components/RouteError'
import { OrgDashboard } from './pages/company/OrgDashboard'
import { Dashboard } from './pages/Dashboard'
import { Forbidden } from './pages/Forbidden'
import { Home } from './pages/Home'
import { OfficeOverview } from './pages/institution/OfficeOverview'
import { Login } from './pages/Login'
import { AuditLog } from './pages/moderation/AuditLog'
import { ReviewQueue } from './pages/moderation/ReviewQueue'
import { RoleManagement } from './pages/moderation/RoleManagement'
import { NotFound } from './pages/NotFound'
import { SectionPlaceholder } from './pages/SectionPlaceholder'
import { StudentDiscover } from './pages/student/StudentDiscover'
import { Register } from './pages/Register'
import { Verify } from './pages/Verify'

const title = (t: string) => ({ title: t }) satisfies RouteHandle

/** A sidebar section: its own page, or a placeholder naming the issue that builds it. */
type Section = { path: string; label: string; issue?: number; element?: ReactNode }

/**
 * One role area from the core wireframes: a guarded sidebar shell whose first section is the
 * home page and whose other sections are placeholders until their issues build them.
 */
function roleArea(
  role: Role,
  path: string,
  navLabel: string,
  home: { label: string; element: ReactNode },
  sections: Section[],
): RouteObject {
  const links = [
    { to: `/${path}`, label: home.label },
    ...sections.map((s) => ({ to: `/${path}/${s.path}`, label: s.label })),
  ]
  return {
    element: <RequireRole roles={[role]} />,
    children: [
      {
        path,
        element: <RoleShell label={navLabel} sections={links} />,
        children: [
          { index: true, element: home.element, handle: title(home.label) },
          ...sections.map((s) => ({
            path: s.path,
            element: s.element ?? <SectionPlaceholder title={s.label} issue={s.issue} />,
            handle: title(s.label),
          })),
        ],
      },
    ],
  }
}

export const roleRoutes: RouteObject[] = [
  roleArea(
    'STUDENT',
    'student',
    'Student sections',
    { label: 'Discover', element: <StudentDiscover /> },
    [
      { path: 'responses', label: 'My responses', issue: 31 },
      { path: 'saved', label: 'Saved opportunities', issue: 23 },
      { path: 'profile', label: 'Profile', issue: 31 },
    ],
  ),
  roleArea(
    'INSTITUTIONAL',
    'institution',
    'Office sections',
    { label: 'Overview', element: <OfficeOverview /> },
    [
      { path: 'opportunities', label: 'My opportunities', issue: 20 },
      { path: 'responses', label: 'Responses', issue: 33 },
      { path: 'profile', label: 'Office profile' },
    ],
  ),
  roleArea(
    'COMPANY',
    'company',
    'Organization sections',
    { label: 'Dashboard', element: <OrgDashboard /> },
    [
      { path: 'opportunities', label: 'Opportunities', issue: 20 },
      { path: 'responses', label: 'Student responses', issue: 33 },
      { path: 'profile', label: 'Organization profile', issue: 16 },
    ],
  ),
  roleArea(
    'MODERATOR',
    'moderation',
    'Administration sections',
    { label: 'Review queue', element: <ReviewQueue /> },
    [
      { path: 'organizations', label: 'Organizations', issue: 16 },
      { path: 'accounts', label: 'Accounts', element: <RoleManagement /> },
      { path: 'reports', label: 'Reports', issue: 37 },
      { path: 'audit', label: 'Audit log', element: <AuditLog /> },
    ],
  ),
]

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <AppLayout />,
    errorElement: <RouteError />,
    children: [
      { index: true, element: <Home />, handle: title('Home') },
      { path: 'login', element: <Login />, handle: title('Sign in') },
      { path: 'register', element: <Register />, handle: title('Create account') },
      { path: 'verify', element: <Verify />, handle: title('Verify your email') },
      { path: 'forbidden', element: <Forbidden />, handle: title('Access denied') },
      {
        element: <RequireAuth />,
        children: [
          { path: 'dashboard', element: <Dashboard />, handle: title('Dashboard') },
          ...roleRoutes,
        ],
      },
      { path: '*', element: <NotFound />, handle: title('Page not found') },
    ],
  },
]

export const router = createBrowserRouter(routes)
