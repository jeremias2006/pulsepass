/**
 * El contexto de sesión: el "contrato" de lo que AuthProvider ofrece al resto de la aplicación.
 *
 * <p>Un contexto de React permite que un valor viaje desde un componente de arriba (el proveedor)
 * hasta cualquier descendiente, sin ir pasándolo de padre a hijo por toda la jerarquía.</p>
 *
 * <p>Está en un archivo aparte de AuthContext.tsx a propósito: así los módulos que solo necesitan
 * el contexto o sus tipos no importan el proveedor entero, y el recargado en caliente de Vite
 * funciona bien (un archivo que exporta componentes y valores a la vez lo rompe).</p>
 */
import { createContext } from 'react'
import type { Rol, Sesion } from './tipos'

/** Lo que cualquier componente puede obtener con `useAuth()`. */
export interface AuthContextValue {
  /** Estado actual: 'cargando' | 'anonimo' | 'autenticado'. */
  sesion: Sesion
  iniciarSesion: (identifier: string, password: string) => Promise<void>
  /** `motivo: 'expirada'` distingue el cierre automático del clic voluntario, para el aviso. */
  cerrarSesion: (motivo?: 'expirada') => void
  /** true si la sesión tiene AL MENOS uno de los roles indicados. */
  tieneRol: (...roles: Rol[]) => boolean
}

// Valor inicial null: significa "no hay proveedor por encima". El hook useAuth lo aprovecha para
// avisar con un error claro en vez de devolver datos vacíos silenciosamente.
export const AuthContext = createContext<AuthContextValue | null>(null)
