import type { ComponentProps, ComponentType } from 'react'
import {
  IconoBalon,
  IconoBirrete,
  IconoEstrella,
  IconoMonitor,
  IconoMusica,
  IconoTemplo,
} from '../../componentes/iconos'
import type { EventCategory } from './tipos'

interface InfoCategoria {
  etiqueta: string
  /** Fondo del panel (claro y oscuro) + color del ícono. */
  fondo: string
  texto: string
  /** Color de la etiqueta de categoría sobre la tarjeta. */
  etiquetaTexto: string
  Icono: ComponentType<ComponentProps<'svg'>>
}

/** DES-02: panel de color + ícono por categoría (la API no entrega imágenes). Mapa completo, sin clases dinámicas.
 *  Claro: fondo -100 e ícono -700/800. Oscuro: fondo -950 e ícono -200 (ver CatalogoOscuro.png). */
export const CATEGORIAS: Record<EventCategory, InfoCategoria> = {
  MUSIC: {
    etiqueta: 'Música',
    fondo: 'bg-indigo-100 dark:bg-indigo-950',
    texto: 'text-indigo-800 dark:text-indigo-200',
    etiquetaTexto: 'text-indigo-700 dark:text-indigo-300',
    Icono: IconoMusica,
  },
  TECHNOLOGY: {
    etiqueta: 'Tecnología',
    fondo: 'bg-sky-100 dark:bg-sky-950',
    texto: 'text-sky-800 dark:text-sky-200',
    etiquetaTexto: 'text-sky-700 dark:text-sky-300',
    Icono: IconoMonitor,
  },
  SPORTS: {
    etiqueta: 'Deportes',
    fondo: 'bg-emerald-100 dark:bg-emerald-950',
    texto: 'text-emerald-800 dark:text-emerald-200',
    etiquetaTexto: 'text-emerald-700 dark:text-emerald-300',
    Icono: IconoBalon,
  },
  CULTURE: {
    etiqueta: 'Cultura',
    fondo: 'bg-rose-100 dark:bg-rose-950',
    texto: 'text-rose-800 dark:text-rose-200',
    etiquetaTexto: 'text-rose-700 dark:text-rose-300',
    Icono: IconoTemplo,
  },
  EDUCATION: {
    etiqueta: 'Educación',
    fondo: 'bg-amber-100 dark:bg-amber-950',
    texto: 'text-amber-800 dark:text-amber-200',
    etiquetaTexto: 'text-amber-700 dark:text-amber-300',
    Icono: IconoBirrete,
  },
  ENTERTAINMENT: {
    etiqueta: 'Entretenimiento',
    fondo: 'bg-violet-100 dark:bg-violet-950',
    texto: 'text-violet-800 dark:text-violet-200',
    etiquetaTexto: 'text-violet-700 dark:text-violet-300',
    Icono: IconoEstrella,
  },
}
