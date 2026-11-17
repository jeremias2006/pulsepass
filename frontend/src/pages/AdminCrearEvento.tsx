import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import Selector from '../componentes/Selector'
import { recordarEventoCreado } from '../funcionalidades/admin/eventosCreados'
import { CATEGORIAS } from '../funcionalidades/eventos/categorias'
import { useCrearEvento } from '../funcionalidades/eventos/hooks'
import {
  CrearEventoSchema,
  EventCategorySchema,
  type CrearEventoForm,
  type CrearEventoSolicitud,
} from '../funcionalidades/eventos/tipos'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { aplicarErroresServidor, mensajeDeError } from '../lib/errores'
import { mananaISO } from '../lib/formato'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

const CAMPOS = [
  'eventCode',
  'name',
  'description',
  'category',
  'eventDate',
  'minimumAge',
  'venueCode',
] as const

export default function AdminCrearEvento() {
  const navigate = useNavigate()
  const sedes = useSedesActivas()
  const crear = useCrearEvento()
  const [generales, setGenerales] = useState<string[]>([])
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<CrearEventoForm>({
    resolver: zodResolver(CrearEventoSchema),
    defaultValues: {
      eventCode: '',
      name: '',
      description: '',
      category: 'MUSIC',
      eventDate: '',
      minimumAge: 0,
      venueCode: '',
    },
  })

  // useWatch (y no watch): es compatible con el React Compiler y evita el aviso del linter.
  const descripcion = useWatch({ control, name: 'description' })

  const onSubmit = handleSubmit((datos) => {
    setGenerales([])
    const { description, ...resto } = datos
    const solicitud: CrearEventoSolicitud = description.trim()
      ? { ...resto, description: description.trim() }
      : resto
    crear.mutate(solicitud, {
      onSuccess: (ev) => {
        recordarEventoCreado({ eventCode: ev.eventCode, name: ev.name })
        void navigate(RUTAS.adminEvento(ev.eventCode), { state: { creado: true } })
      },
      onError: (e: unknown) => {
        if (e instanceof ApiError && e.status === 403) {
          void navigate(RUTAS.sinPermisos)
          return
        }
        const detalles = aplicarErroresServidor(e, setError, CAMPOS) // 400 → junto a cada campo
        setGenerales(e instanceof ApiError && e.status === 400 ? detalles : [mensajeDeError(e)]) // 409 → arriba
      },
    })
  })

  return (
    <div className="space-y-4">
      <Link
        to={RUTAS.adminEventos}
        className="text-sm font-medium text-marca-700 dark:text-marca-500"
      >
        ← Eventos
      </Link>
      <h1 className="font-titulo text-2xl font-bold">Nuevo evento</h1>
      {generales.length > 0 && (
        <Aviso tono="error">
          {generales.map((g) => (
            <p key={g}>{g}</p>
          ))}
        </Aviso>
      )}
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        <Campo
          etiqueta="Código del evento"
          error={errors.eventCode?.message}
          {...register('eventCode')}
        />
        <Campo etiqueta="Nombre" error={errors.name?.message} {...register('name')} />
        <div>
          <label htmlFor="descripcion" className="mb-1 block text-sm font-medium">
            Descripción
          </label>
          <textarea
            id="descripcion"
            rows={3}
            className="block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"
            {...register('description')}
          />
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            {descripcion.length}/1000
          </p>
          {errors.description && (
            <p role="alert" className="text-sm text-red-700 dark:text-red-400">
              {errors.description.message}
            </p>
          )}
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Selector etiqueta="Categoría" error={errors.category?.message} {...register('category')}>
            {EventCategorySchema.options.map((c) => (
              <option key={c} value={c}>
                {CATEGORIAS[c].etiqueta}
              </option>
            ))}
          </Selector>
          <Campo
            etiqueta="Edad mínima"
            type="number"
            min={0}
            error={errors.minimumAge?.message}
            {...register('minimumAge', { valueAsNumber: true })}
          />
        </div>
        {/* El backend solo guarda la FECHA (LocalDate) y exige que sea posterior a hoy. */}
        <Campo
          etiqueta="Fecha"
          type="date"
          min={mananaISO()}
          error={errors.eventDate?.message}
          {...register('eventDate')}
        />
        <Selector etiqueta="Sede" error={errors.venueCode?.message} {...register('venueCode')}>
          <option value="">Selecciona una sede…</option>
          {sedes.data?.map((s) => (
            <option key={s.code} value={s.code}>
              {s.name} ({s.code})
            </option>
          ))}
        </Selector>
        <div className="flex justify-end gap-3">
          <Link
            to={RUTAS.adminEventos}
            className="inline-flex min-h-11 items-center rounded-lg border border-slate-300 px-4 font-semibold"
          >
            Cancelar
          </Link>
          <Boton type="submit" cargando={crear.isPending}>
            Crear evento
          </Boton>
        </div>
      </form>
    </div>
  )
}
