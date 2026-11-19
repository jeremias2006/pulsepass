/**
 * Acceso a la sesión desde cualquier componente. Lo usan la barra de navegación, los guardias de
 * ruta y el panel de compra, entre otros.
 */
import { useContext } from 'react'
import { AuthContext, type AuthContextValue } from './contexto'

/**
 * Devuelve la sesión y sus operaciones.
 *
 * <p>La comprobación convierte un `null` silencioso en un error explícito: si alguien usara este
 * hook fuera de `<AuthProvider>`, sin ella recibiría datos vacíos y el fallo aparecería mucho
 * después y en otro sitio. Además, gracias al `throw`, TypeScript sabe que a partir de ahí el
 * valor nunca es null, y quien lo use no tiene que comprobarlo.</p>
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
