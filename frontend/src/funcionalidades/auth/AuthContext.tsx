/**
 * Proveedor de la sesión: el dueño del token y del usuario autenticado.
 *
 * <p>Es el componente más delicado del frontend, porque concentra el ciclo de vida completo del
 * JWT:</p>
 * <ol>
 *   <li><b>Nace</b> en `iniciarSesion`: se llama a /auth/login, se guarda el token y se pide
 *       /auth/me para conocer los roles.</li>
 *   <li><b>Se usa</b> en cada petición: `configurarHttp` le entrega a lib/http una función para
 *       leer el token vigente.</li>
 *   <li><b>Se restaura</b> al recargar la pestaña, desde sessionStorage, siempre confirmándolo
 *       contra el backend.</li>
 *   <li><b>Muere</b> de tres formas: al cumplirse los 15 minutos (temporizador local), cuando el
 *       backend responde 401, o al cerrar sesión a mano.</li>
 * </ol>
 *
 * <p>El token vive en DOS sitios con propósitos distintos: en memoria (`tokenRef`), que es de
 * donde lo toma cada petición, y en sessionStorage, que solo sirve para sobrevivir a un F5.</p>
 */
import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { configurarHttp } from '../../lib/http'
import { CLAVE_EVENTOS_CREADOS } from '../admin/eventosCreados'
import { login, obtenerMe } from './api'
import { AuthContext, type AuthContextValue } from './contexto'
import { CLAVE_AVISO, CLAVE_SESION, TokenGuardadoSchema, type Rol, type Sesion } from './tipos'

/**
 * Lee el token guardado, o null si no hay ninguno utilizable.
 *
 * <p>Tres filtros, porque el contenido de sessionStorage es un dato externo que cualquiera puede
 * editar desde las herramientas del navegador: que exista, que tenga la forma esperada
 * (`safeParse`) y que no esté vencido. El `try/catch` cubre el caso de un JSON corrupto, que
 * haría fallar a `JSON.parse` antes de llegar a Zod.</p>
 *
 * <p>Descartar aquí un token vencido ahorra una llamada inútil al backend.</p>
 */
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
  // Estado inicial: si había token guardado se arranca en 'cargando' mientras se confirma contra
  // /auth/me; si no, directamente 'anonimo'. Sin el estado intermedio, los guardias de ruta
  // echarían al usuario al login en cada recarga.
  const [sesion, setSesion] = useState<Sesion>(
    guardado ? { estado: 'cargando' } : { estado: 'anonimo' },
  )
  // useRef y no useState: el token lo lee lib/http en el momento de cada petición, y cambiarlo
  // no debe provocar un repintado. Un ref guarda un valor mutable sin redibujar nada.
  const tokenRef = useRef<string | null>(null) // el token vive en memoria y en sessionStorage

  /**
   * Cierra la sesión y borra todo rastro de ella.
   *
   * <p>`queryClient.clear()` es imprescindible: la caché puede contener los tickets o el perfil
   * de la persona que acaba de salir, y sin limpiarla el siguiente usuario de ese navegador los
   * vería. Ese es también el motivo del orden de los proveedores en main.tsx.</p>
   *
   * <p>No se llama a ningún endpoint de logout: no existe. La API es STATELESS, el servidor no
   * guarda los tokens emitidos, así que no hay nada que cerrar del otro lado. El token se tira
   * aquí y caduca solo a los 15 minutos.</p>
   */
  const cerrarSesion = useCallback(
    (motivo?: 'expirada') => {
      tokenRef.current = null
      sessionStorage.removeItem(CLAVE_SESION)
      sessionStorage.removeItem(CLAVE_EVENTOS_CREADOS)
      queryClient.clear() // PRD: cerrar sesión limpia la caché
      setSesion({ estado: 'anonimo' })
      // El aviso se deja escrito para que lo lea la pantalla de login tras la redirección, y
      // allí se borra. Es la forma de pasar un mensaje a través de un cambio de ruta.
      if (motivo === 'expirada') sessionStorage.setItem(CLAVE_AVISO, 'expirada')
    },
    [queryClient],
  )

  // Enchufa el token y el 401 en lib/http.
  // Este es el punto de unión entre la sesión y el transporte: lib/http no puede importar este
  // archivo (sería una dependencia circular, porque aquí se importa `login`, que usa `http`),
  // así que es la sesión la que le entrega las dos funciones que necesita.
  useEffect(() => {
    configurarHttp({
      obtenerToken: () => tokenRef.current,
      alNoAutorizado: () => cerrarSesion('expirada'),
    })
  }, [cerrarSesion])

  // Restaura la sesión al recargar la pestaña (solo si había un token vigente).
  // No basta con que el token no haya caducado: se confirma contra /auth/me, porque la cuenta
  // pudo ser desactivada o el usuario borrado. Si esa llamada falla, se vuelve a anónimo.
  // Además es la vía por la que se recuperan los roles, que el token no lleva dentro.
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
  // Es una cortesía, no una medida de seguridad: evita que el usuario descubra que su sesión
  // caducó al recibir un 401 en mitad de una compra. Aunque este temporizador fallara (pestaña
  // dormida, por ejemplo), el backend rechazaría el token igual.
  const expiraEn = sesion.estado === 'autenticado' ? sesion.expiraEn : null
  useEffect(() => {
    if (expiraEn === null) return
    // Math.max(..., 0) protege de un retraso negativo si el token ya venció.
    const id = window.setTimeout(() => cerrarSesion('expirada'), Math.max(expiraEn - Date.now(), 0))
    // Función de limpieza: cancela el temporizador si el componente se desmonta o si cambia el
    // vencimiento. Sin ella quedarían temporizadores sueltos cerrando sesiones que ya no existen.
    return () => window.clearTimeout(id)
  }, [expiraEn, cerrarSesion])

  /**
   * Inicia sesión: /auth/login y, acto seguido, /auth/me.
   *
   * <p>Son dos llamadas porque el JWT no lleva los roles: el backend los consulta en la base de
   * datos en cada petición, de modo que quitar un rol surte efecto de inmediato. /auth/me sirve
   * además de comprobación: si falla, el token se descarta y no se da la sesión por iniciada.</p>
   *
   * <p>`expiresIn` llega en segundos (900) y se convierte a marca de tiempo en milisegundos,
   * que es con lo que trabaja Date.now().</p>
   */
  const iniciarSesion = useCallback(async (identifier: string, password: string) => {
    const auth = await login({ identifier, password })
    const vence = Date.now() + auth.expiresIn * 1000
    // Se pone el token en memoria ANTES de llamar a obtenerMe, porque lib/http lo necesita para
    // esa misma petición.
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
      // "alguno de", no "todos": tieneRol('STAFF','ADMIN') es cierto con cualquiera de los dos,
      // igual que hasAnyRole("STAFF","ADMIN") en SecurityConfig.
      tieneRol: (...roles: Rol[]) =>
        sesion.estado === 'autenticado' && roles.some((r) => sesion.usuario.roles.includes(r)),
    }),
    // useMemo evita crear un objeto nuevo en cada repintado: como este valor viaja por el
    // contexto, un objeto nuevo obligaría a redibujar a TODOS los componentes que usan useAuth.
    [sesion, iniciarSesion, cerrarSesion],
  )

  // React 19: el contexto se usa directamente como proveedor (<AuthContext value=…>).
  return <AuthContext value={valor}>{children}</AuthContext>
}
