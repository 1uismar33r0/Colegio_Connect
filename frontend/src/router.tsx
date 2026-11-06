import { createBrowserRouter } from 'react-router'
import { RequireAuth, RequireRole } from './auth/guards'
import { AppLayout } from './components/AppLayout'
import { Dashboard } from './pages/Dashboard'
import { Forbidden } from './pages/Forbidden'
import { Home } from './pages/Home'
import { Login } from './pages/Login'
import { NotFound } from './pages/NotFound'
import { RoleArea } from './pages/RoleArea'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'login', element: <Login /> },
      { path: 'forbidden', element: <Forbidden /> },
      {
        element: <RequireAuth />,
        children: [
          { path: 'dashboard', element: <Dashboard /> },
          {
            element: <RequireRole roles={['STUDENT']} />,
            children: [{ path: 'student/*', element: <RoleArea title="Student" /> }],
          },
          {
            element: <RequireRole roles={['INSTITUTIONAL']} />,
            children: [{ path: 'institution/*', element: <RoleArea title="Institution" /> }],
          },
          {
            element: <RequireRole roles={['COMPANY']} />,
            children: [{ path: 'company/*', element: <RoleArea title="Company" /> }],
          },
          {
            element: <RequireRole roles={['MODERATOR']} />,
            children: [{ path: 'moderation/*', element: <RoleArea title="Moderation" /> }],
          },
        ],
      },
      { path: '*', element: <NotFound /> },
    ],
  },
])
