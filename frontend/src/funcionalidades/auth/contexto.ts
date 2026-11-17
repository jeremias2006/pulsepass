import { createContext } from 'react'
import type { Rol, Sesion } from './tipos'

export interface AuthContextValue {
  sesion: Sesion
  iniciarSesion: (identifier: string, password: string) => Promise<void>
  cerrarSesion: (motivo?: 'expirada') => void
  tieneRol: (...roles: Rol[]) => boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)
