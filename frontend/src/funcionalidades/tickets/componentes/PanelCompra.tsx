import { useState } from 'react'
import { Link } from 'react-router'
import Boton from '../../../componentes/Boton'
import Tarjeta from '../../../componentes/Tarjeta'
import { RUTAS } from '../../../lib/rutas'
import { useAuth } from '../../auth/hooks'
import type { Evento } from '../../eventos/tipos'
import { ETIQUETA_TIPO } from '../etiquetas'
import { TicketTypeSchema, type TicketType } from '../tipos'
import ModalConfirmarCompra from './ModalConfirmarCompra'

/**
 * Panel de compra de la pantalla de detalle del evento: elegir el tipo de entrada y confirmar.
 *
 * <p>Nada de lo que decide este componente es seguridad. Ocultar el panel a quien no es USER o
 * deshabilitar el botón en un evento agotado solo evita un viaje inútil y un mensaje de error.
 * Quien de verdad impide la compra es el backend, con `hasRole("USER")` en SecurityConfig y las
 * siete reglas BR-001 a BR-007 de TicketService.</p>
 *
 * <p>Tres situaciones, tres comportamientos: anónimo ve un enlace al login (que luego lo
 * devuelve aquí), USER ve el botón de compra, y STAFF o ADMIN no ven el panel.</p>
 */
/** Solo UX: el backend decide (rol USER, estado, edad, cupo). STAFF/ADMIN no ven el panel (FR-SEC-TKT-01). */
export default function PanelCompra({ evento }: { evento: Evento }) {
  const { sesion, tieneRol } = useAuth()
  const [tipo, setTipo] = useState<TicketType>('GENERAL')
  const [modal, setModal] = useState(false)

  // Se comprueba el estado 'autenticado' antes del rol: a quien no ha entrado sí se le muestra
  // el panel, con el enlace al login. Quien ya entró y no es USER no tiene nada que hacer aquí.
  if (sesion.estado === 'autenticado' && !tieneRol('USER')) return null
  // Solo se vende lo que está PUBLISHED: ni DRAFT, ni SOLD_OUT, ni CANCELLED, ni FINISHED (BR-004).
  const vendible = evento.status === 'PUBLISHED'

  return (
    <>
      <Tarjeta>
        <h2 className="font-titulo text-lg font-semibold">Entradas</h2>
        <fieldset className="mt-3 space-y-2">
          <legend className="text-sm font-medium">Tipo de entrada</legend>
          {TicketTypeSchema.options.map((t) => (
            <label key={t} className="flex min-h-11 items-center gap-3">
              <input
                type="radio"
                name="tipo"
                value={t}
                checked={tipo === t}
                onChange={() => setTipo(t)}
              />
              {ETIQUETA_TIPO[t]}
            </label>
          ))}
        </fieldset>

        {sesion.estado === 'autenticado' ? (
          <Boton className="mt-4 w-full" disabled={!vendible} onClick={() => setModal(true)}>
            Comprar entrada
          </Boton>
        ) : (
          // Anónimo: va a /login y vuelve al evento (state.desde).
          <Link
            to={RUTAS.login}
            state={{ desde: RUTAS.evento(evento.eventCode) }}
            className="mt-4 inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-marca-600 px-4 font-semibold text-white"
          >
            Inicia sesión para comprar
          </Link>
        )}
        {!vendible && (
          <p className="mt-2 text-sm text-amber-700 dark:text-amber-400">
            La compra no está disponible para este evento.
          </p>
        )}
        {/* El precio no se muestra aquí porque el frontend no lo calcula: lo fija el servidor
            (TicketPriceCalculator) a partir del tipo elegido. Se avisa para que no sorprenda. */}
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
          El precio lo define el sistema al confirmar.
        </p>
      </Tarjeta>

      {sesion.estado === 'autenticado' && (
        <ModalConfirmarCompra
          abierto={modal}
          evento={evento}
          tipo={tipo}
          emailSesion={sesion.usuario.email}
          onCerrar={() => setModal(false)}
        />
      )}
    </>
  )
}
