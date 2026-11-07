import { useContext } from 'react'
import { AuthContext } from './AuthContext'

export function useAuth() {
  const csx = useContext(AuthContext)
  if (!csx) throw new Error('useAuth must be used within an <AuthProvider>')
  return csx
}
