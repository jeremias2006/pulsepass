import { Link, isRouteErrorResponse, useNavigate, useRouteError } from 'react-router'
import Boton from '../componentes/Boton'
import { RUTAS } from '../lib/rutas'

/**
 * ErrorBoundary de la ruta raíz. Mensaje seguro: nunca stack trace ni detalles internos.
 *
 * <p>Es la red de seguridad del frontend: si un componente lanza una excepción no prevista, en
 * lugar de dejar la pantalla en blanco se muestra esto. Cumple el papel que `GlobalExceptionHandler`
 * cumple en el backend, y con el mismo criterio: al usuario, un mensaje genérico; el detalle se
 * queda en la consola del navegador.</p>
 *
 * <p>Mostrar la traza del error revelaría nombres de componentes y rutas internas, que es
 * información útil para quien busque vulnerabilidades.</p>
 */
export default function ErrorPagina() {
  const error = useRouteError()
  const navigate = useNavigate()
  const mensaje =
    isRouteErrorResponse(error) && error.status === 404
      ? 'No encontramos esta página.'
      : 'Ocurrió un error inesperado. Inténtalo más tarde.'
  return (
    <section role="alert" className="mx-auto max-w-md py-16 text-center">
      <h1 className="font-titulo text-2xl font-bold">¡Ups! Algo salió mal.</h1>
      <p className="mt-3 text-slate-600 dark:text-slate-400">{mensaje}</p>
      <div className="mt-6 flex justify-center gap-3">
        <Boton onClick={() => void navigate(0)}>Reintentar</Boton>
        <Link
          to={RUTAS.inicio}
          className="inline-flex min-h-11 items-center rounded-lg border border-slate-300 px-4 font-semibold"
        >
          Volver al inicio
        </Link>
      </div>
    </section>
  )
}
