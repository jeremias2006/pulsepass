import { useState, type FormEvent } from 'react'
import BotonCopiar from '../componentes/BotonCopiar'
import Aviso from '../componentes/Aviso'
import { BadgeEstadoTicket } from '../componentes/Badge'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import Cargando from '../componentes/Cargando'
import CodigoTexto from '../componentes/CodigoTexto'
import Tarjeta from '../componentes/Tarjeta'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/etiquetas'
import { useBuscarTicket, useUsarTicket } from '../funcionalidades/tickets/hooks'
import ListaPorValidar from '../funcionalidades/tickets/ListaPorValidar'
import { mensajeDeError } from '../lib/errores'
import { ApiError } from '../lib/http'

function mensajeBusqueda(e: unknown): string {
    if (e instanceof ApiError && e.status === 404) return 'Ticket no encontrado'
    if (e instanceof ApiError && e.status === 403) return 'Sin permisos'
    return mensajeDeError(e)
}

export default function ValidacionIngreso() {
    const [texto, setTexto] = useState('')
    const [codigo, setCodigo] = useState<string | null>(null) // el código ya enviado a buscar
    const [vacio, setVacio] = useState(false)
    const [exito, setExito] = useState(false)
    const ticket = useBuscarTicket(codigo)
    const usar = useUsarTicket()

    const buscar = (e: FormEvent) => {
        e.preventDefault()
        const c = texto.trim()
        usar.reset()
        setExito(false)
        if (!c) {
            setVacio(true)
            return
        }
        setVacio(false)
        if (c === codigo) void ticket.refetch()
        else setCodigo(c)
    }

    // Desde la lista «Tickets por validar»: carga el ticket en la tarjeta de arriba para confirmarlo.
    const seleccionar = (c: string) => {
        usar.reset()
        setExito(false)
        setVacio(false)
        setTexto(c)
        if (c === codigo) void ticket.refetch()
        else setCodigo(c)
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    const marcar = () => {
        if (!codigo) return
        usar.mutate(codigo, {
            onSuccess: () => {
                setExito(true)
                setTexto('') // tras el éxito el campo se limpia (listo para el siguiente ingreso)
            },
            // Si otra persona ya lo usó, se vuelve a consultar para mostrar el estado real.
            onError: () => void ticket.refetch(),
        })
    }

    return (
        <div className="space-y-6">
            <h1 className="font-titulo text-2xl font-bold">Validación de ingreso</h1>
            <form onSubmit={buscar} className="flex items-end gap-3">
                <div className="flex-1">
                    <Campo
                        etiqueta="Código de ticket"
                        value={texto}
                        onChange={(e) => setTexto(e.target.value)}
                        error={vacio ? 'Ingresa el código del ticket' : undefined}
                        autoFocus
                        autoComplete="off"
                    />
                </div>
                <Boton type="submit">Buscar</Boton>
            </form>

            {ticket.isFetching && <Cargando etiqueta="Buscando ticket…" />}
            {ticket.isError && <Aviso tono="error">{mensajeBusqueda(ticket.error)}</Aviso>}

            {ticket.data && !ticket.isFetching && (
                <Tarjeta className="space-y-3">
                    <div className="flex items-start justify-between gap-3">
                        <h2 className="font-titulo text-xl font-semibold">{ticket.data.eventName}</h2>
                        <BadgeEstadoTicket estado={ticket.data.status} />
                    </div>
                    <p className="flex flex-wrap items-center gap-x-2 text-slate-600 dark:text-slate-400">
                        <CodigoTexto>{ticket.data.ticketCode}</CodigoTexto>
                        <BotonCopiar valor={ticket.data.ticketCode} etiqueta="código del ticket" /> ·{' '}
                        {ETIQUETA_TIPO[ticket.data.type]} · Titular: {ticket.data.userEmail}
                    </p>
                    <Boton
                        cargando={usar.isPending}
                        disabled={ticket.data.status !== 'PAID'}
                        onClick={marcar}
                    >
                        Marcar como usado
                    </Boton>
                </Tarjeta>
            )}

            {exito && <Aviso tono="exito">Ingreso registrado.</Aviso>}
            {usar.isError && <Aviso tono="advertencia">{mensajeDeError(usar.error)}</Aviso>}

            <hr className="border-slate-200 dark:border-slate-700" />
            <ListaPorValidar onSeleccionar={seleccionar} />
        </div>
    )
}