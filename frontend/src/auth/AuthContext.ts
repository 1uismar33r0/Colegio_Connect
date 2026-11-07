import { createContext } from 'react'
import type { UserResponse } from '../api/client'
import type { Role } from './roles'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous' | 'error'

export type AuthState = {
  status: AuthStatus
  user: UserResponse | null
  roles: Role[]
  error: Error | null
}

export type AuthContextValue = AuthState & {
  refresh: () => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
