import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import CampoContrasena from '../componentes/CampoContrasena'
import { useAuth } from '../funcionalidades/auth/hooks'
import { CLAVE_AVISO, LoginSchema, type LoginForm } from '../funcionalidades/auth/tipos'
import { ApiError } from '../lib/http'
import { desdeSeguro, RUTAS } from '../lib/rutas'

const vieneDeRegistro = (state: unknown): boolean =>
  typeof state === 'object' && state !== null && 'registrado' in state && state.registrado === true

export default function Login() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // El aviso "sesión expirada" lo deja AuthProvider en sessionStorage; se lee una vez y se borra.
  const [expirada] = useState(() => sessionStorage.getItem(CLAVE_AVISO) === 'expirada')
  useEffect(() => {
    sessionStorage.removeItem(CLAVE_AVISO)
  }, [])

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(LoginSchema) })

  const onSubmit = handleSubmit(async (datos) => {
    try {
      await iniciarSesion(datos.identifier, datos.password)
      await navigate(desdeSeguro(location.state), { replace: true })
    } catch (e: unknown) {
      // 401 → mensaje genérico: nunca se distingue "usuario inexistente" de "contraseña incorrecta".
      setError('root', {
        message:
          e instanceof ApiError && e.status === 401
            ? 'Credenciales inválidas'
            : 'No pudimos iniciar sesión. Inténtalo más tarde.',
      })
    }
  })

  return (
    <div className="space-y-4">
      <h1 className="font-titulo text-2xl font-bold">Inicia sesión</h1>
      {expirada && <Aviso tono="info">Tu sesión expiró. Inicia sesión de nuevo.</Aviso>}
      {vieneDeRegistro(location.state) && (
        <Aviso tono="exito">Cuenta creada. Ahora inicia sesión.</Aviso>
      )}
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <Campo
          etiqueta="Usuario o correo"
          autoComplete="username"
          error={errors.identifier?.message}
          {...register('identifier')}
        />
        <CampoContrasena
          etiqueta="Contraseña"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        {errors.root && <Aviso tono="error">{errors.root.message}</Aviso>}
        <Boton type="submit" cargando={isSubmitting} className="w-full">
          Ingresar
        </Boton>
      </form>
      <p className="text-sm">
        ¿No tienes cuenta?{' '}
        <Link to={RUTAS.registro} className="font-medium text-marca-700 dark:text-marca-500">
          Regístrate
        </Link>
      </p>
    </div>
  )
}
