import { useState, type FormEvent } from 'react'
import BotonCopiar from '../componentes/BotonCopiar'
import { Link, useNavigate } from 'react-router'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import CodigoTexto from '../componentes/CodigoTexto'
import Tarjeta from '../componentes/Tarjeta'
import { leerEventosCreados } from '../funcionalidades/admin/eventosCreados'
import { RUTAS } from '../lib/rutas'

export default function AdminEventos() {
  const navigate = useNavigate()
  const [codigo, setCodigo] = useState('')
  const [creados] = useState(leerEventosCreados) // GAP-01: solo los creados en esta pestaña

  const abrir = (e: FormEvent) => {
    e.preventDefault()
    const c = codigo.trim()
    if (c) void navigate(RUTAS.adminEvento(c)) // la página de gestión hace el GET y muestra el 404
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="font-titulo text-2xl font-bold">Administración de eventos</h1>
        <Link
          to={RUTAS.adminNuevoEvento}
          className="inline-flex min-h-11 items-center rounded-lg bg-marca-600 px-4 text-sm font-semibold text-white"
        >
          + Nuevo evento
        </Link>
      </div>

      <form onSubmit={abrir} className="flex items-end gap-3">
        <div className="flex-1">
          <Campo
            etiqueta="Buscar evento por código"
            value={codigo}
            onChange={(e) => setCodigo(e.target.value)}
          />
        </div>
        <Boton type="submit">Abrir</Boton>
      </form>

      <section aria-labelledby="creados" className="space-y-3">
        <h2 id="creados" className="font-titulo text-lg font-semibold">
          Creados en esta sesión
        </h2>
        {creados.length === 0 && (
          <p className="text-sm text-slate-600 dark:text-slate-400">Aún no has creado eventos.</p>
        )}
        {creados.map((e) => (
          <Tarjeta key={e.eventCode} className="flex items-center justify-between gap-3">
            <span>
              <CodigoTexto>{e.eventCode}</CodigoTexto> · {e.name}
            </span>
            <BotonCopiar valor={e.eventCode} etiqueta="código del evento" />
            <Link
              to={RUTAS.adminEvento(e.eventCode)}
              className="font-medium text-marca-700 dark:text-marca-500"
            >
              Gestionar
            </Link>
          </Tarjeta>
        ))}
      </section>

      <Aviso tono="info">
        La API no ofrece un listado completo de eventos (GAP-01): los borradores se abren por
        código.
      </Aviso>
    </div>
  )
}
