import { useState } from 'react'
import Aviso from '../../../componentes/Aviso'
import Boton from '../../../componentes/Boton'
import Modal from '../../../componentes/Modal'
import { mensajeDeError } from '../../../lib/errores'
import { useCancelarTicket } from '../hooks'
import type { Ticket } from '../tipos'

/**
 * Botón de cancelar con su confirmación. Se usa dentro de TarjetaTicket y de DetalleTicket.
 *
 * <p>Una acción destructiva siempre pide confirmación antes de ejecutarse: cancelar un ticket no
 * se puede deshacer, no existe un endpoint para revertirlo.</p>
 *
 * <p>El componente se oculta solo cuando el ticket no está PAID, pero esa comprobación es de
 * comodidad: si alguien forzara la petición, el backend respondería 409 por las reglas BR-010 a
 * BR-012 (solo tickets pagados, y no después de la fecha del evento).</p>
 */
/** Botón + modal de confirmación. Solo se ofrece si status === 'PAID' (el backend sigue siendo la autoridad: 409). */
export default function CancelarTicket({ ticket }: { ticket: Ticket }) {
  const [abierto, setAbierto] = useState(false)
  const cancelar = useCancelarTicket()

  if (ticket.status !== 'PAID') return null

  // reset() limpia el error de un intento anterior: si el usuario cierra el modal tras un fallo
  // y lo vuelve a abrir, no debe encontrarse el mensaje antiguo esperándolo.
  const cerrar = () => {
    cancelar.reset()
    setAbierto(false)
  }

  return (
    <>
      <Boton variante="peligro" onClick={() => setAbierto(true)}>
        Cancelar ticket
      </Boton>
      <Modal abierto={abierto} titulo="Cancelar ticket" onCerrar={cerrar}>
        <p className="mb-4 text-sm">
          ¿Seguro que quieres cancelar el ticket{' '}
          <span className="font-mono">{ticket.ticketCode}</span>?
        </p>
        {cancelar.isError && (
          <div className="mb-4">
            <Aviso tono="error">{mensajeDeError(cancelar.error)}</Aviso>
          </div>
        )}
        <div className="flex justify-end gap-3">
          <Boton variante="secundario" onClick={cerrar}>
            Volver
          </Boton>
          <Boton
            variante="peligro"
            cargando={cancelar.isPending}
            onClick={() => cancelar.mutate(ticket.ticketCode, { onSuccess: cerrar })}
          >
            Sí, cancelar
          </Boton>
        </div>
      </Modal>
    </>
  )
}
