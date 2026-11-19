import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import CampoContrasena from '../componentes/CampoContrasena'
import { aplicarErroresServidor, mensajeDeError } from '../lib/errores'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import { useRegistrarUsuario } from '../funcionalidades/usuarios/hooks'
import { RegistroSchema, type RegistroForm } from '../funcionalidades/usuarios/tipos'

/**
 * Registro de usuario (ruta `/registro`). Caso de uso: crear una cuenta para poder comprar.
 *
 * <p><b>Acceso:</b> solo sin sesión (dentro de `SoloAnonimo`).<br>
 * <b>Consume:</b> POST /users, que es el endpoint de registro (lo atiende UserController, no
 * AuthController).</p>
 *
 * <p>La cuenta se crea siempre con rol USER: el servidor lo fija en el código, así que desde
 * aquí es imposible registrarse como STAFF o ADMIN. Esas cuentas solo las crea
 * `LocalAccountsSeeder` al arrancar.</p>
 *
 * <p>Ejemplo de las tres validaciones del proyecto: Zod en el formulario (al instante), Bean
 * Validation en el DTO del backend (400) y las reglas BR-USER en el Service (409, por ejemplo
 * si el correo ya existe).</p>
 */
// Los campos del formulario, para repartir los errores que devuelve el backend en `details`:
// los que coinciden con un campo se marcan sobre él; el resto se muestran como aviso general.
const CAMPOS = ['username', 'email', 'firstName', 'lastName', 'birthDate', 'password'] as const

export default function Registro() {
  const navigate = useNavigate()
  const registrar = useRegistrarUsuario()
  const [generales, setGenerales] = useState<string[]>([])
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegistroForm>({ resolver: zodResolver(RegistroSchema) })

  const onSubmit = handleSubmit((datos) => {
    setGenerales([])
    registrar.mutate(datos, {
      // GAP-07: el registro no emite token → flujo registro → login.
      onSuccess: () => void navigate(RUTAS.login, { state: { registrado: true } }),
      onError: (e: unknown) => {
        if (e instanceof ApiError && e.status === 409) {
          // Genérico a propósito: el backend incluye el correo/usuario en su mensaje.
          setGenerales(['El usuario o correo ya está registrado'])
          return
        }
        const detalles = aplicarErroresServidor(e, setError, CAMPOS)
        setGenerales(e instanceof ApiError && e.status === 400 ? detalles : [mensajeDeError(e)])
      },
    })
  })

  return (
    <div className="space-y-4">
      <h1 className="font-titulo text-2xl font-bold">Crea tu cuenta</h1>
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <Campo
          etiqueta="Nombre de usuario"
          autoComplete="username"
          error={errors.username?.message}
          {...register('username')}
        />
        <Campo
          etiqueta="Correo electrónico"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register('email')}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Nombres"
            autoComplete="given-name"
            error={errors.firstName?.message}
            {...register('firstName')}
          />
          <Campo
            etiqueta="Apellidos"
            autoComplete="family-name"
            error={errors.lastName?.message}
            {...register('lastName')}
          />
        </div>
        <Campo
          etiqueta="Fecha de nacimiento"
          type="date"
          autoComplete="bday"
          error={errors.birthDate?.message}
          {...register('birthDate')}
        />
        <CampoContrasena
          etiqueta="Contraseña (mín. 12)"
          autoComplete="new-password"
          error={errors.password?.message}
          {...register('password')}
        />
        {generales.length > 0 && (
          <Aviso tono="error">
            {generales.map((g) => (
              <p key={g}>{g}</p>
            ))}
          </Aviso>
        )}
        <Boton type="submit" cargando={registrar.isPending} className="w-full">
          Crear cuenta
        </Boton>
      </form>
      <p className="text-sm">
        ¿Ya tienes cuenta?{' '}
        <Link to={RUTAS.login} className="font-medium text-marca-700 dark:text-marca-500">
          Inicia sesión
        </Link>
      </p>
    </div>
  )
}
