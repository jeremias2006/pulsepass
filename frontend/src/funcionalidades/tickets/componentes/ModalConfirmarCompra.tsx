import { useNavigate } from 'react-router'
import Aviso from '../../../componentes/Aviso'
import Boton from '../../../componentes/Boton'
import Modal from '../../../componentes/Modal'
import { mensajeDeError } from '../../../lib/errores'
import { ApiError } from '../../../lib/http'
import { RUTAS } from '../../../lib/rutas'
import type { Evento } from '../../eventos/tipos'
import { ETIQUETA_TIPO } from '../etiquetas'
import { useComprarTicket } from '../hooks'
import type { TicketType } from '../tipos'

interface Props {
  abierto: boolean
  evento: Evento
  tipo: TicketType
  emailSesion: string // SIEMPRE de la sesión, nunca de un campo editable (anti-IDOR)
  onCerrar: () => void
}

export default function ModalConfirmarCompra({
  abierto,
  evento,
  tipo,
  emailSesion,
  onCerrar,
}: Props) {
  const navigate = useNavigate()
  const compra = useComprarTicket()

  const confirmar = () =>
    compra.mutate(
      { userEmail: emailSesion, eventCode: evento.eventCode, type: tipo },
      {
        onSuccess: (t) => void navigate(RUTAS.confirmacionCompra(t.ticketCode)),
        // 401: lib/http ya cerró la sesión; se vuelve al login y luego al evento.
        onError: (e: unknown) => {
          if (e instanceof ApiError && e.status === 401)
            void navigate(RUTAS.login, { state: { desde: RUTAS.evento(evento.eventCode) } })
        },
      },
    )

  const cerrar = () => {
    compra.reset()
    onCerrar()
  }

  return (
    <Modal abierto={abierto} titulo="Confirmar compra" onCerrar={cerrar}>
      <dl className="mb-4 space-y-1 text-sm">
        <div>
          <dt className="inline font-medium">Evento: </dt>
          <dd className="inline">{evento.name}</dd>
        </div>
        <div>
          <dt className="inline font-medium">Tipo: </dt>
          <dd className="inline">{ETIQUETA_TIPO[tipo]}</dd>
        </div>
        <div>
          <dt className="inline font-medium">Cuenta: </dt>
          <dd className="inline">{emailSesion}</dd>
        </div>
      </dl>
      {/* El 409 (edad, cupo, estado…) se muestra DENTRO del modal, sin cerrarlo (DES-10). */}
      {compra.isError && (
        <div className="mb-4">
          <Aviso tono="error">{mensajeDeError(compra.error)}</Aviso>
        </div>
      )}
      <div className="flex justify-end gap-3">
        <Boton variante="secundario" onClick={cerrar}>
          Cancelar
        </Boton>
        <Boton cargando={compra.isPending} onClick={confirmar}>
          Confirmar
        </Boton>
      </div>
    </Modal>
  )
}
