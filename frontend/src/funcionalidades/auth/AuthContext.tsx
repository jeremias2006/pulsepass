import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { configurarHttp } from '../../lib/http'
import { CLAVE_EVENTOS_CREADOS } from '../admin/eventosCreados'
import { login, obtenerMe } from './api'
import { AuthContext, type AuthContextValue } from './contexto'
import { CLAVE_AVISO, CLAVE_SESION, TokenGuardadoSchema, type Rol, type Sesion } from './tipos'

function leerGuardado() {
  try {
    const crudo = sessionStorage.getItem(CLAVE_SESION)
    if (!crudo) return null
    const r = TokenGuardadoSchema.safeParse(JSON.parse(crudo))
    return r.success && r.data.expiraEn > Date.now() ? r.data : null // vencido → se descarta
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [guardado] = useState(leerGuardado) // se lee una sola vez, al montar
  const [sesion, setSesion] = useState<Sesion>(
    guardado ? { estado: 'cargando' } : { estado: 'anonimo' },
  )
  const tokenRef = useRef<string | null>(null) // el token vive en memoria y en sessionStorage

  const cerrarSesion = useCallback(
    (motivo?: 'expirada') => {
      tokenRef.current = null
      sessionStorage.removeItem(CLAVE_SESION)
      sessionStorage.removeItem(CLAVE_EVENTOS_CREADOS)
      queryClient.clear() // PRD: cerrar sesión limpia la caché
      setSesion({ estado: 'anonimo' })
      if (motivo === 'expirada') sessionStorage.setItem(CLAVE_AVISO, 'expirada')
    },
    [queryClient],
  )

  // Enchufa el token y el 401 en lib/http.
  useEffect(() => {
    configurarHttp({
      obtenerToken: () => tokenRef.current,
      alNoAutorizado: () => cerrarSesion('expirada'),
    })
  }, [cerrarSesion])

  // Restaura la sesión al recargar la pestaña (solo si había un token vigente).
  useEffect(() => {
    if (!guardado) {
      sessionStorage.removeItem(CLAVE_SESION)
      return
    }
    tokenRef.current = guardado.token
    obtenerMe()
      .then((usuario) => setSesion({ estado: 'autenticado', ...guardado, usuario }))
      .catch(() => {
        tokenRef.current = null
        sessionStorage.removeItem(CLAVE_SESION)
        setSesion({ estado: 'anonimo' })
      })
  }, [guardado])

  // Cierre local al vencer el token (15 min).
  const expiraEn = sesion.estado === 'autenticado' ? sesion.expiraEn : null
  useEffect(() => {
    if (expiraEn === null) return
    const id = window.setTimeout(() => cerrarSesion('expirada'), Math.max(expiraEn - Date.now(), 0))
    return () => window.clearTimeout(id)
  }, [expiraEn, cerrarSesion])

  const iniciarSesion = useCallback(async (identifier: string, password: string) => {
    const auth = await login({ identifier, password })
    const vence = Date.now() + auth.expiresIn * 1000
    tokenRef.current = auth.accessToken
    try {
      const usuario = await obtenerMe() // los roles vienen de /auth/me; el JWT no se decodifica
      sessionStorage.setItem(
        CLAVE_SESION,
        JSON.stringify({ token: auth.accessToken, expiraEn: vence }),
      )
      setSesion({ estado: 'autenticado', token: auth.accessToken, expiraEn: vence, usuario })
    } catch (e: unknown) {
      tokenRef.current = null
      throw e
    }
  }, [])

  const valor = useMemo<AuthContextValue>(
    () => ({
      sesion,
      iniciarSesion,
      cerrarSesion,
      tieneRol: (...roles: Rol[]) =>
        sesion.estado === 'autenticado' && roles.some((r) => sesion.usuario.roles.includes(r)),
    }),
    [sesion, iniciarSesion, cerrarSesion],
  )

  // React 19: el contexto se usa directamente como proveedor (<AuthContext value=…>).
  return <AuthContext value={valor}>{children}</AuthContext>
}
