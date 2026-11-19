/**
 * Bloque de error con opción de reintentar. Es lo que muestran las pantallas cuando
 * `isError` es cierto.
 *
 * <p>Delega el texto en `mensajeDeError`, de modo que la decisión de qué contar al usuario
 * según el código HTTP vive en un solo sitio (lib/errores) y no repartida por las pantallas.
 * Admite además un `mensaje` propio para los casos en que la pantalla sabe algo más preciso.</p>
 */
import { mensajeDeError } from '../lib/errores'
import Boton from './Boton'

interface EstadoErrorProps {
  error?: unknown
  mensaje?: string
  onReintentar?: () => void
}

export default function EstadoError({ error, mensaje, onReintentar }: EstadoErrorProps) {
  return (
    <div
      role="alert"
      className="rounded-xl border border-red-200 bg-red-50 p-6 text-center text-red-900"
    >
      <p>{mensaje ?? mensajeDeError(error)}</p>
      {onReintentar && (
        <Boton variante="secundario" className="mt-4" onClick={onReintentar}>
          Reintentar
        </Boton>
      )}
    </div>
  )
}
