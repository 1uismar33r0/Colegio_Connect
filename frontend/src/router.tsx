import { createBrowserRouter } from 'react-router'
import { RequireAuth, RequireRole } from './auth/guards'
import { AppLayout, type RouteHandle } from './components/AppLayout'
import { RouteError } from './components/RouteError'
import { Dashboard } from './pages/Dashboard'
import { Forbidden } from './pages/Forbidden'
import { Home } from './pages/Home'
import { Login } from './pages/Login'
import { NotFound } from './pages/NotFound'
import { RoleArea } from './pages/RoleArea'

const title = (t: string) => ({ title: t }) satisfies RouteHandle

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    errorElement: <RouteError />,
    children: [
      { index: true, element: <Home />, handle: title('Home') },
      { path: 'login', element: <Login />, handle: title('Sign in') },
      { path: 'forbidden', element: <Forbidden />, handle: title('Access denied') },
      {
        element: <RequireAuth />,
        children: [
          { path: 'dashboard', element: <Dashboard />, handle: title('Dashboard') },
          {
            element: <RequireRole roles={['STUDENT']} />,
            children: [
              {
                path: 'student/*',
                element: <RoleArea title="Student" />,
                handle: title('Student'),
              },
            ],
          },
          {
            element: <RequireRole roles={['INSTITUTIONAL']} />,
            children: [
              {
                path: 'institution/*',
                element: <RoleArea title="Institution" />,
                handle: title('Institution'),
              },
            ],
          },
          {
            element: <RequireRole roles={['COMPANY']} />,
            children: [
              {
                path: 'company/*',
                element: <RoleArea title="Company" />,
                handle: title('Company'),
              },
            ],
          },
          {
            element: <RequireRole roles={['MODERATOR']} />,
            children: [
              {
                path: 'moderation/*',
                element: <RoleArea title="Moderation" />,
                handle: title('Moderation'),
              },
            ],
          },
        ],
      },
      { path: '*', element: <NotFound />, handle: title('Page not found') },
    ],
  },
])
