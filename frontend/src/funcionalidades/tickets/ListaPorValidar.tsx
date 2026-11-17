import { useState, type FormEvent } from 'react'
import BotonCopiar from '../../componentes/BotonCopiar'
import { BadgeEstadoTicket } from '../../componentes/Badge'
import Boton from '../../componentes/Boton'
import Campo from '../../componentes/Campo'
import Cargando from '../../componentes/Cargando'
import CodigoTexto from '../../componentes/CodigoTexto'
import EstadoError from '../../componentes/EstadoError'
import EstadoVacio from '../../componentes/EstadoVacio'
import Tabla, { type Columna } from '../../componentes/Tabla'
import { esNoDisponible } from '../../lib/errores'
import { ETIQUETA_TIPO } from './etiquetas'
import { useTicketsPagadosPorEvento } from './hooks'
import type { Ticket } from './tipos'

/**
 * Mejora propia (no está en el PRD §9.14): lista los tickets PAID de un evento para
 * encontrar un ticket sin tener su código a mano. El botón «Seleccionar» de cada fila
 * lo carga en la tarjeta de búsqueda, donde se confirma con «Marcar como usado»
 * (un solo camino de validación). Usa GET /api/events/{code}/tickets/paid.
 */
export default function ListaPorValidar({
                                            onSeleccionar,
                                        }: {
    onSeleccionar: (ticketCode: string) => void
}) {
    const [texto, setTexto] = useState('')
    const [eventCode, setEventCode] = useState<string | undefined>()
    const [filtro, setFiltro] = useState('')
    const q = useTicketsPagadosPorEvento(eventCode)

    const cargar = (e: FormEvent) => {
        e.preventDefault()
        const c = texto.trim()
        setFiltro('')
        if (c) setEventCode(c)
    }

    const f = filtro.trim().toLowerCase()
    const filas = (q.data ?? []).filter(
        (t) => !f || t.ticketCode.toLowerCase().includes(f) || t.userEmail.toLowerCase().includes(f),
    )

    const columnas: Columna<Ticket>[] = [
        {
            encabezado: 'Código',
            celda: (t) => (
                <span className="flex items-center gap-1">
          <CodigoTexto>{t.ticketCode}</CodigoTexto>
          <BotonCopiar valor={t.ticketCode} etiqueta="código del ticket" />
        </span>
            ),
        },
        { encabezado: 'Tipo', celda: (t) => ETIQUETA_TIPO[t.type] },
        { encabezado: 'Titular', celda: (t) => t.userEmail },
        { encabezado: 'Estado', celda: (t) => <BadgeEstadoTicket estado={t.status} /> },
        {
            encabezado: 'Acción',
            celda: (t) => (
                <Boton variante="secundario" onClick={() => onSeleccionar(t.ticketCode)}>
                    Seleccionar
                </Boton>
            ),
        },
    ]

    return (
        <section aria-labelledby="titulo-por-validar" className="space-y-4">
            <h2 id="titulo-por-validar" className="font-titulo text-xl font-semibold">
                Tickets por validar
            </h2>
            <form onSubmit={cargar} className="flex items-end gap-3">
                <div className="flex-1">
                    <Campo
                        etiqueta="Código del evento"
                        value={texto}
                        onChange={(e) => setTexto(e.target.value)}
                        autoComplete="off"
                    />
                </div>
                <Boton type="submit" variante="secundario">
                    Ver tickets
                </Boton>
            </form>

            {eventCode && q.isPending && <Cargando />}
            {q.isError &&
                (esNoDisponible(q.error) ? (
                    <EstadoVacio mensaje="Evento no encontrado" />
                ) : (
                    <EstadoError error={q.error} onReintentar={() => void q.refetch()} />
                ))}
            {q.data &&
                (q.data.length === 0 ? (
                    <EstadoVacio mensaje="No hay tickets por validar" />
                ) : (
                    <>
                        <Campo
                            etiqueta="Filtrar por código o titular"
                            value={filtro}
                            onChange={(e) => setFiltro(e.target.value)}
                            autoComplete="off"
                        />
                        <p aria-live="polite" className="text-sm text-slate-600 dark:text-slate-400">
                            Por validar: {filas.length}
                        </p>
                        {filas.length === 0 ? (
                            <EstadoVacio mensaje="Ningún ticket coincide con el filtro" />
                        ) : (
                            <Tabla columnas={columnas} filas={filas} clave={(t) => t.ticketCode} />
                        )}
                    </>
                ))}
        </section>
    )
}