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
